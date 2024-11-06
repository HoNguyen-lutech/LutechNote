package com.grownapp.noteappgrown.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.icu.util.Calendar
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.adapter.ListCategoryAdapter
import com.grownapp.noteappgrown.adapter.ListColorAdapter
import com.grownapp.noteappgrown.adapter.ListNoteAdapter
import com.grownapp.noteappgrown.database.NoteDatabase
import com.grownapp.noteappgrown.databinding.ActivityEditNoteBinding
import com.grownapp.noteappgrown.listeners.OnColorClickListener
import com.grownapp.noteappgrown.listeners.OnItemClickListener
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.models.Note
import com.grownapp.noteappgrown.models.NoteCategoryCrossRef
import com.grownapp.noteappgrown.repository.CategoryRepository
import com.grownapp.noteappgrown.repository.NoteCategoryRepository
import com.grownapp.noteappgrown.repository.NoteRepository
import com.grownapp.noteappgrown.viewmodel.CategoryViewModel
import com.grownapp.noteappgrown.viewmodel.CategoryViewModelFactory
import com.grownapp.noteappgrown.viewmodel.NoteCategoryViewModel
import com.grownapp.noteappgrown.viewmodel.NoteCategoryViewModelFactory
import com.grownapp.noteappgrown.viewmodel.NoteViewModel
import com.grownapp.noteappgrown.viewmodel.NoteViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class EditNoteActivity : AppCompatActivity(), OnColorClickListener {
    private val binding: ActivityEditNoteBinding by lazy {
        ActivityEditNoteBinding.inflate(layoutInflater)
    }
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var currentContent: String
    private val textUndo = mutableListOf<Pair<String, Int>>()
    private val textRedo = mutableListOf<Pair<String, Int>>()
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var noteCategoryViewModel: NoteCategoryViewModel
    private lateinit var noteAdapter: ListNoteAdapter
    private lateinit var categoryAdapter: ListCategoryAdapter
    private lateinit var colorAdapter: ListColorAdapter
    private lateinit var categories: List<Category>
    private var isUndo = false
    private var selectedColor: String? = null
    private var selectedFormatTextColor: String? = null
    private var selectedTextSize: Int = 18
    private val colors = listOf(
        "#FFCDD2","#F8BBD0","#E1BEE7","#D1C4E9","#C5CAE9",
        "#BBDEFB","#B3E5FC","#B2EBF2","#B2DFDB","#C8E6C9",
        "#DCEDC8","#F0F4C3","#FFECB3","#FFE0B2","#FFCCBC",
        "#D7CCC8","#F5F5F5","#CFD8DC","#FF8A80","#FF80AB"
    )
    private val formatColor = listOf(
        "#000000", "#444444", "#888888", "#CDCDCD",
        "#FFFFFF", "#FE0000", "#01FF02", "#0000FE",
        "#FFFF00", "#00FFFF", "#FF01FF", "#FE0000",
        "#FE1D01", "#FF3900", "#FF5700", "#FF7300",
        "#FF9000", "#FFAD01", "#FFCB00", "#FFE601",
        "#F9FF01", "#DFFE02", "#BFFF00", "#A4FF01",
        "#87FF00", "#69FF01", "#4CFF00", "#30FF00",
        "#13FF00", "#01FF0C", "#00FF27", "#00FF43",
        "#00FF61", "#00FF7D", "#00FE9B", "#01FFB7",
        "#00FFD7", "#01FFF2", "#01F1FE", "#01D4FF",
        "#00B6FF", "#0099FF", "#007DFE", "#0060FF",
        "#0043FE", "#0027FF", "#000aFF", "#1400FF",
        "#3000FE", "#4d00FE", "#6a00FF", "#8800FF",
        "#A501FF", "#BF00FE", "#DD00FE", "#FA00FF",
        "#F923E5", "#FE00CB", "#FF02AD", "#FF0090",
        "#FE0072", "#FD0156", "#FF003C", "#FF011D"
    )
    companion object{
        private const val CREATE_FILE_REQUEST_CODE = 1
    }

    private lateinit var selectColorTv: TextView
    private var colorFillOrText: Int = -1
    private var styleStates = mutableMapOf(
        "BOLD" to false,
        "ITALIC" to false,
        "UNDERLINE" to false,
        "STRIKETHROUGH" to false,
        "FILLCOLOR" to false,
        "TEXTCOLOR" to false,
        "TEXTSIZE" to false
    )
    private var currentTextColor: Int? = null
    private var currentBackgroundColor: Int? = null
    private var currentTextSize: Int? = null

    private val handler = Handler(Looper.getMainLooper())
    private var previousStart = -1
    private var previousEnd = -1
    private var sort = intent.getIntExtra("sort", -1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setUpViewModel()
        loadNote()
        formattingBarAction()
        binding.topAppBar.setNavigationOnClickListener {
            saveNote()
            finish()
        }
        val save = SpannableString(binding.topAppBar.menu.findItem(R.id.Save).title)
        save.setSpan(ForegroundColorSpan(Color.WHITE), 0, save.length, 0)
        binding.topAppBar.menu.findItem(R.id.Save).title = save
        val undo = SpannableString(binding.topAppBar.menu.findItem(R.id.Undo).title)
        undo.setSpan(ForegroundColorSpan(Color.WHITE), 0, undo.length, 0)
        binding.topAppBar.menu.findItem(R.id.Undo).title = undo
        binding.topAppBar.overflowIcon?.setTint(Color.WHITE)
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId){
                R.id.Save -> {
                    saveNote()
                    true
                }
                R.id.Undo -> {
                    undoNote()
                    true
                }
                R.id.Redo -> {
                    redoNote()
                    true
                }
                R.id.undo_all -> {
                    undoAll()
                    true
                }
                R.id.Share -> {
                    shareNote()
                    true
                }
                R.id.export_text_a_file -> {
                    exportNoteToTextFile()
                    true
                }
                R.id.delete -> {
                    deleteNote()
                    true
                }
                R.id.search_note -> {
                    true
                }
                R.id.categorize_note -> {
                    showCategorizeDialog()
                    true
                }
                R.id.Colorize -> {
                    showColorPickerDialog()
                    true
                }
                R.id.switch_to_read_mode -> {
                    false
                }
                R.id.print -> {
                    false
                }
                R.id.show_formatting_bar -> {
                    binding.formattingBar.visibility = View.VISIBLE
                    true
                }
                R.id.showInfo -> {
                    showInfoDialog()
                    true
                }else -> {
                    false
                }
            }
        }


        binding.edtContent.addTextChangedListener(object : TextWatcher{
            private var startPosition = 0
            private var endPosition = 0
            override fun beforeTextChanged(p0: CharSequence?, start: Int, count: Int, after: Int) {
                if(!isUndo){
                    textUndo.add(
                        Pair(binding.edtContent.text.toString(), binding.edtContent.selectionStart)
                    )
                }else{
                    isUndo = false
                }

                startPosition = start
                endPosition = start + after
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                applyCurrentStylesForContent(startPosition, endPosition)
                if(currentTextColor != null){
                    applyTextColor(startPosition, endPosition)
                }
                if(currentBackgroundColor != null){
                    applyBackgroundColor(startPosition, endPosition)
                }
                if(currentTextSize != null){
                    applyTextSize(startPosition, endPosition)
                }
            }
        })
//        binding.edtTitle.addTextChangedListener(object : TextWatcher{
//            private var startPosition = 0
//            private var endPosition = 0
//            override fun beforeTextChanged(p0: CharSequence?, start: Int, count: Int, after: Int) {
//                if(!isUndo){
//                    textUndo.add(
//                        Pair(binding.edtTitle.text.toString(), binding.edtTitle.selectionStart)
//                    )
//                }else{
//                    isUndo = false
//                }
//
//                startPosition = start
//                endPosition = start + after
//            }
//
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//
//            }
//
//            override fun afterTextChanged(p0: Editable?) {
//                applyCurrentStylesForTitle(startPosition, endPosition)
//                if(currentTextColor != null){
//                    applyTextColor(startPosition, endPosition)
//                }
//                if(currentBackgroundColor != null){
//                    applyBackgroundColor(startPosition, endPosition)
//                }
//                if(currentTextSize != null){
//                    applyTextSize(startPosition, endPosition)
//                }
//            }
//        })
        startSelectionMonitor()
    }

    private fun toggleStyleForContent(style: String){
        styleStates[style] = !(styleStates[style]?:false)
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd
        applyCurrentStylesForContent(start, end)
    }

//    private fun toggleStyleForTitle(style: String){
//        styleStates[style] = !(styleStates[style]?:false)
//        val start = binding.edtTitle.selectionStart
//        val end = binding.edtTitle.selectionEnd
//        applyCurrentStylesForTitle(start, end)
//    }

    private fun applyCurrentStylesForContent(start: Int, end: Int) {
        val text = binding.edtContent.text
        if (text is Spannable) {
            var isCursorOnly = start == end
            if (styleStates["BOLD"] == true) {
                binding.bold.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                if (isCursorOnly) {
                    if (text.getSpans(start, end, StyleSpan::class.java)
                            .none { it.style == Typeface.BOLD }
                    ) {
                        text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_MARK_MARK)
                    }
                } else {
                    text.getSpans(start, end, StyleSpan::class.java).forEach {
                        if (it.style == Typeface.BOLD) text.removeSpan(it)
                    }
                    text.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else {
                binding.bold.setBackgroundColor(Color.TRANSPARENT)
                if (!isCursorOnly) {
                    text.getSpans(start, end, StyleSpan::class.java).forEach {
                        if (it.style == Typeface.BOLD) text.removeSpan(it)
                    }
                }
            }
            if (styleStates["ITALIC"] == true) {
                binding.italic.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                if (isCursorOnly) {
                    if (text.getSpans(start, end, StyleSpan::class.java)
                            .none { it.style == Typeface.ITALIC }
                    ) {
                        text.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            start,
                            end,
                            Spannable.SPAN_MARK_MARK
                        )
                    }
                } else {
                    text.getSpans(start, end, StyleSpan::class.java).forEach {
                        if (it.style == Typeface.ITALIC) text.removeSpan(it)
                    }
                    text.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else {
                binding.italic.setBackgroundColor(Color.TRANSPARENT)
                if (!isCursorOnly) {
                    text.getSpans(start, end, StyleSpan::class.java).forEach {
                        if (it.style == Typeface.ITALIC) text.removeSpan(it)
                    }
                }
            }
            if (styleStates["UNDERLINE"] == true) {
                binding.underline.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                if (isCursorOnly) {
                    if (text.getSpans(start, end, UnderlineSpan::class.java).isEmpty()) {
                        text.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_MARK_MARK)
                    }
                } else {
                    text.getSpans(start, end, UnderlineSpan::class.java)
                        .forEach { text.removeSpan(it) }
                    text.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                binding.underline.setBackgroundColor(Color.TRANSPARENT)
                if (!isCursorOnly) {
                    text.getSpans(start, end, UnderlineSpan::class.java)
                        .forEach { text.removeSpan(it) }
                }
            }
            if (styleStates["STRIKETHROUGH"] == true) {
                binding.strikeThrough.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                if (isCursorOnly) {
                    if (text.getSpans(start, end, StrikethroughSpan::class.java).isEmpty()) {
                        text.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_MARK_MARK)
                    }
                } else {
                    text.setSpan(
                        StrikethroughSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            } else {
                binding.strikeThrough.setBackgroundColor(Color.TRANSPARENT)
                if (!isCursorOnly) {
                    text.getSpans(start, end, StrikethroughSpan::class.java)
                        .forEach { text.removeSpan(it) }
                }
            }
            if(styleStates["FILLCOLOR"] == true){
//                onFillColorSelected(currentBackgroundColor!!)
            }
            if(styleStates["TEXTCOLOR"] == true){
                currentTextColor?.let { color ->
                    text.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                }
            }
        }
    }
//    private fun applyCurrentStylesForTitle(start: Int, end: Int) {
//        val text = binding.edtTitle.text
//        if (text is Spannable) {
//            var isCursorOnly = start == end
//            if (styleStates["BOLD"] == true) {
//                binding.bold.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
//                if (isCursorOnly) {
//                    if (text.getSpans(start, end, StyleSpan::class.java)
//                            .none { it.style == Typeface.BOLD }
//                    ) {
//                        text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_MARK_MARK)
//                    }
//                } else {
//                    text.getSpans(start, end, StyleSpan::class.java).forEach {
//                        if (it.style == Typeface.BOLD) text.removeSpan(it)
//                    }
//                    text.setSpan(
//                        StyleSpan(Typeface.BOLD),
//                        start,
//                        end,
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                }
//            } else {
//                binding.bold.setBackgroundColor(Color.TRANSPARENT)
//                if (!isCursorOnly) {
//                    text.getSpans(start, end, StyleSpan::class.java).forEach {
//                        if (it.style == Typeface.BOLD) text.removeSpan(it)
//                    }
//                }
//            }
//            if (styleStates["ITALIC"] == true) {
//                binding.italic.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
//                if (isCursorOnly) {
//                    if (text.getSpans(start, end, StyleSpan::class.java)
//                            .none { it.style == Typeface.ITALIC }
//                    ) {
//                        text.setSpan(
//                            StyleSpan(Typeface.ITALIC),
//                            start,
//                            end,
//                            Spannable.SPAN_MARK_MARK
//                        )
//                    }
//                } else {
//                    text.getSpans(start, end, StyleSpan::class.java).forEach {
//                        if (it.style == Typeface.ITALIC) text.removeSpan(it)
//                    }
//                    text.setSpan(
//                        StyleSpan(Typeface.ITALIC),
//                        start,
//                        end,
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                }
//            } else {
//                binding.italic.setBackgroundColor(Color.TRANSPARENT)
//                if (!isCursorOnly) {
//                    text.getSpans(start, end, StyleSpan::class.java).forEach {
//                        if (it.style == Typeface.ITALIC) text.removeSpan(it)
//                    }
//                }
//            }
//            if (styleStates["UNDERLINE"] == true) {
//                binding.underline.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
//                if (isCursorOnly) {
//                    if (text.getSpans(start, end, UnderlineSpan::class.java).isEmpty()) {
//                        text.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_MARK_MARK)
//                    }
//                } else {
//                    text.getSpans(start, end, UnderlineSpan::class.java)
//                        .forEach { text.removeSpan(it) }
//                    text.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//                }
//            } else {
//                binding.underline.setBackgroundColor(Color.TRANSPARENT)
//                if (!isCursorOnly) {
//                    text.getSpans(start, end, UnderlineSpan::class.java)
//                        .forEach { text.removeSpan(it) }
//                }
//            }
//            if (styleStates["STRIKETHROUGH"] == true) {
//                binding.strikeThrough.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
//                if (isCursorOnly) {
//                    if (text.getSpans(start, end, StrikethroughSpan::class.java).isEmpty()) {
//                        text.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_MARK_MARK)
//                    }
//                } else {
//                    text.setSpan(
//                        StrikethroughSpan(),
//                        start,
//                        end,
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                }
//            } else {
//                binding.strikeThrough.setBackgroundColor(Color.TRANSPARENT)
//                if (!isCursorOnly) {
//                    text.getSpans(start, end, StrikethroughSpan::class.java)
//                        .forEach { text.removeSpan(it) }
//                }
//            }
//        }
//    }

    private fun setUpViewModel() {
        val noteRepository = NoteRepository(NoteDatabase(this))
        val viewModelProviderFactory = NoteViewModelFactory(application, noteRepository)
        noteViewModel = ViewModelProvider(this, viewModelProviderFactory)[NoteViewModel::class.java]
        val categoryRepository = CategoryRepository(NoteDatabase(this))
        val cateViewModelProviderFactory = CategoryViewModelFactory(application, categoryRepository)
        categoryViewModel = ViewModelProvider(this, cateViewModelProviderFactory)[CategoryViewModel::class.java]
        val noteCategoryRepository = NoteCategoryRepository(NoteDatabase(this))
        val noteCategoryViewModelFactory = NoteCategoryViewModelFactory(application, noteCategoryRepository)
        noteCategoryViewModel = ViewModelProvider(this, noteCategoryViewModelFactory)[NoteCategoryViewModel::class.java]
        noteAdapter = ListNoteAdapter(this, object : OnItemClickListener{
            override fun onNoteClick(note: Note, isChoose: Boolean) {

            }

            override fun onNoteLongClick(note: Note) {

            }
        })
        categoryAdapter = ListCategoryAdapter(this)
        colorAdapter = ListColorAdapter(colors, this)
        this.let {
            categoryViewModel.getAllCategory().observe(this){category ->
                categoryAdapter.differ.submitList(category)
                categories = categoryAdapter.differ.currentList
            }
        }
    }

    private fun undoAll() {
        binding.edtContent.setText(currentContent)
    }

    private fun undoNote() {
        if(textUndo.isNotEmpty()){
            isUndo = true
            val(previousText, previousCursorPosition) = textUndo.removeLast()
            textRedo.add(Pair(previousText, previousCursorPosition))
            binding.edtContent.setText(previousText)
            binding.edtContent.setSelection((previousCursorPosition))
        }
    }

    private fun redoNote() {
        if(textRedo.isNotEmpty()){
            val (previousText, previousCursorPosition) = textRedo.removeLast()
            textUndo.add(Pair(previousText, previousCursorPosition))
            binding.edtContent.setText(previousText)
            binding.edtContent.setSelection(previousCursorPosition)
        }
    }

    private fun saveNote() {
        val id = intent.getIntExtra("id", 0)
        val created = intent.getStringExtra("created")
        val color = noteViewModel.getColor(id)
        val noteTitle = binding.edtTitle.text.toString()
        val noteContent = Html.toHtml(binding.edtContent.text)
        if(id == 0){
            val note = Note(
                noteViewModel.getLatestId(),
                noteTitle,
                noteContent,
                getCurrentTime(),
                created!!,
                color,
                false
            )
            noteViewModel.updateNote(note)
        }else{
            val note = Note(id, noteTitle, noteContent, getCurrentTime(), created!!, color, false)
            noteViewModel.updateNote(note)
        }
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val formattedDate = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(calendar.time)
        return formattedDate
    }

    private fun exportNoteToTextFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "${binding.edtTitle.text}.txt")
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            data?.data?.also { uri ->
                this.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val content = binding.edtContent.text.toString()
                    outputStream.write(content.toByteArray())
                }
            }
            Toast.makeText(this, "1 note(s) exported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCategorizeDialog() {
        val checkedItem = BooleanArray(categories.size)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Selected category")
            .setPositiveButton("OK"){dialog, which ->
                val selectedCategories = mutableListOf<Category>()
                val unSelectedCategories = mutableListOf<Category>()
                for (i in categories.indices){
                    if(checkedItem[i]){
                        selectedCategories.add(categories[i])
                    }else{
                        unSelectedCategories.add(categories[i])
                    }
                }
                val id = intent.getIntExtra("id", 0)
                for(categoryId in selectedCategories.map { it.id }){
                    noteCategoryViewModel.addNoteCategory(NoteCategoryCrossRef(id, categoryId))
                }
                Toast.makeText(this, "Updated categories", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }.setNegativeButton("CANCEL"){dialog, which ->
                dialog.dismiss()
            }.setMultiChoiceItems(
                categories.map { it.categoryName }.toTypedArray(),
                checkedItem
            ){dialog, which, isChecked ->
                checkedItem[which] = isChecked
            }
        builder.create().show()
    }

    private fun deleteNote() {
        val message = if(binding.edtTitle.text.toString() == ""){
            "Untitled"
        }else{
            binding.edtTitle.text
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            .setMessage("The '$message' note will be deleted. Are you sure?")
            .setPositiveButton("Delete"){dialog, which ->
                val id = intent.getIntExtra("id", 0)
                val title = intent.getStringExtra("title")
                val content = intent.getStringExtra("content")
                val created = intent.getStringExtra("created")
                val time = intent.getStringExtra("time")
                val note = Note(
                    id,
                    title.toString(),
                    content.toString(),
                    time.toString(),
                    created.toString(),
                    null,
                    false
                )
                noteViewModel.deleteNote(note)
                finish()
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun showInfoDialog() {
        val words = binding.edtContent.text.toString().trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val characters = binding.edtContent.text.toString().count()
        val charactersWithoutWhitespaces = binding.edtContent.text.toString().filter { !it.isWhitespace() }.length
        val created = intent.getStringExtra("created")
        val time = intent.getStringExtra("time")
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            .setMessage("Words: $words \nWrapped lines: 1 \nCharacters: $characters \nCharacters without whitespaces: $charactersWithoutWhitespaces \nCreated at: $created \nLast saved at: $time")
            .setPositiveButton("OK"){dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun shareNote() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, null))
    }

    private fun showColorPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_colorize, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rcvColor)
        val removeColor = dialogView.findViewById<Button>(R.id.removeColorBtn)
        var isRemove = false
        recyclerView.layoutManager = GridLayoutManager(this, 5)
        colorAdapter = ListColorAdapter(colors, this)
        recyclerView.adapter = colorAdapter
        removeColor.setOnClickListener {
            isRemove = true
        }
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("CANCEL"){dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton("OK"){dialog, which ->
                if(isRemove) selectedColor = null
                handleOkButtonClick()
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun handleOkButtonClick() {
        selectedColor.let { color ->
            if(selectedColor.isNullOrEmpty()){
                binding.editNote.setBackgroundResource(R.drawable.bg_edit_note)
                binding.appBar.setBackgroundColor(ContextCompat.getColor(this,R.color.brownEarth))
                binding.appBar.setStatusBarForegroundColor(ContextCompat.getColor(this,R.color.white))
            }else{
                val backgroundDrawable = GradientDrawable()
                backgroundDrawable.setColor(Color.parseColor(color ?: "#FFFFFF"))
                backgroundDrawable.setStroke(4, ContextCompat.getColor(this,R.color.brownEarth))
                binding.editNote.background = backgroundDrawable
                binding.appBar.background = backgroundDrawable
            }
            val id = intent.getIntExtra("id", 0)
            val title = intent.getStringExtra("title")
            val content = intent.getStringExtra("content")
            val created = intent.getStringExtra("created")
            val time = intent.getStringExtra("time")

            val note = Note(
                id,
                title.toString(),
                content.toString(),
                time.toString(),
                created.toString(),
                color,
                false
            )
            Log.d("TAG", "handleOkButtonClick: $note")
            noteViewModel.updateNote(note)
        }
        noteAdapter.notifyDataSetChanged()
    }

    private fun loadNote() {
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title")
        val color = intent.getStringExtra("color")
        val content: String = if(id == 0){
            intent.getStringExtra("content").toString()
        }else{
            noteViewModel.getNoteById(id).content
        }
        if(color != null){
            val backgroundDrawable = GradientDrawable()
            backgroundDrawable.setColor(Color.parseColor(color))
            backgroundDrawable.setStroke(4, ContextCompat.getColor(this, R.color.brownEarth))
            binding.editNote.background = backgroundDrawable
            binding.appBar.background = backgroundDrawable
            binding.main.background = backgroundDrawable
        }
        currentContent = content
        binding.edtTitle.setText(title)
        binding.edtContent.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY))
    }

    private fun formattingBarAction() {
        binding.bold.setOnClickListener {
            toggleStyleForContent("BOLD")
//            if(isBoldEnabled){
//                val start = binding.edtContent.selectionStart
//                val end = binding.edtContent.selectionEnd
//                applyStyleToRange(Typeface.BOLD, binding.edtContent,start,end)
//                binding.bold.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
//            }else{
//                applyStyle(Typeface.BOLD, binding.edtContent)
//                isBoldEnabled = false
//                binding.bold.setBackgroundColor(Color.TRANSPARENT)
//            }
        }
        binding.italic.setOnClickListener {
            toggleStyleForContent("ITALIC")
        }
        binding.underline.setOnClickListener {
            toggleStyleForContent("UNDERLINE")
        }
        binding.strikeThrough.setOnClickListener {
            toggleStyleForContent("STRIKETHROUGH")
        }
        binding.fillColor.setOnClickListener {
            colorFillOrText = 0
            showFormatColorPickerDialog(binding.fillColor)

        }
        binding.textColor.setOnClickListener {
            colorFillOrText = 1
            showFormatColorPickerDialog(binding.textColor)
        }
        binding.textSize.setOnClickListener {
            showFormatTextSizeDialogForContent(binding.edtContent)
        }
        binding.closeBtn.setOnClickListener {
            binding.formattingBar.visibility = View.GONE
        }
    }

    private fun showFormatColorPickerDialog(imageView: ImageView) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_format_color_text, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rcvFormatColor)
        val removeColor = dialogView.findViewById<Button>(R.id.removeFormatColorBtn)
        selectColorTv = dialogView.findViewById<TextView>(R.id.selectColorTv)
        var isRemove = false
        recyclerView.layoutManager = GridLayoutManager(this, 8)
        colorAdapter = ListColorAdapter(formatColor, this)
        recyclerView.adapter = colorAdapter
        removeColor.setOnClickListener {
            isRemove = true
            removeColor(imageView)
        }
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("CANCEL"){dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton("OK"){dialog, _ ->
                if(isRemove) {
//                    selectedFormatTextColor = null
//                    currentBackgroundColor = null
                    dialog.dismiss()
                }else{
                    setupColorForEditText(binding.edtContent, imageView)
                    dialog.dismiss()
                }

            }
        builder.create().show()
    }

    private fun removeColor(imageView: ImageView){
        val spannable = binding.edtContent.text as Spannable
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd
        if(imageView == binding.textColor){
            if(start < end){
                spannable.getSpans(start, end, ForegroundColorSpan::class.java).forEach { span ->
                    spannable.removeSpan(span)
                    binding.textColor.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            currentTextColor = null
        }else if(imageView == binding.fillColor){
            if(start < end){
                spannable.getSpans(start, end, BackgroundColorSpan::class.java).forEach { span ->
                    spannable.removeSpan(span)
                    binding.fillColor.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            currentBackgroundColor = null
        }
    }

    private fun setupColorForEditText(editText: EditText, imageView: ImageView) {
        selectedFormatTextColor.let { color ->
            if(selectedFormatTextColor.isNullOrEmpty()){
//                changeColorSelected(editText, "#000000", imageView)

            }else{
                changeColorSelected(editText, color, imageView)
//                imageView.setBackgroundColor(Color.parseColor(color))
            }
        }
    }

    private fun applyBackgroundColorToSelection(editText: EditText, color: Int) {
        val spannable = editText.text as Spannable
        val start = editText.selectionStart
        val end = editText.selectionEnd

        // Xóa các màu nền cũ trong vùng chọn (nếu có)
        spannable.getSpans(start, end, BackgroundColorSpan::class.java).forEach { span ->
            spannable.removeSpan(span)
        }

        // Áp dụng màu nền mới
        spannable.setSpan(BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun changeColorSelected(editText: EditText, color: String?, imageView: ImageView) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val spannable = editText.text as Spannable
        val colorInt = parseColor(color!!)
        if(imageView == binding.fillColor){
            onBackgroundColorSelected(Color.parseColor(color))
            val spans = spannable.getSpans(start, end, BackgroundColorSpan::class.java)
            spannable.setSpan(BackgroundColorSpan(colorInt), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if(spans.isNotEmpty()){
                for(span in spans){
                    spannable.removeSpan(span)
                }
            }else{
                onFillColorSelected(colorInt)
                spannable.setSpan(
                    BackgroundColorSpan(colorInt),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            Log.d("TAG","setupColorForEditText: fill")

        }
        if(imageView == binding.textColor){
            onTextColorSelected(Color.parseColor(color))
            val spans = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
            spannable.setSpan(ForegroundColorSpan(colorInt), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if(spans.isNotEmpty()){
                for(span in spans){
                    spannable.removeSpan(span)
                }
            }else{
                spannable.setSpan(
                    ForegroundColorSpan(colorInt),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            Log.d("TAG", "setupColorForEditText: text")
        }
    }

    private fun showFormatTextSizeDialogForContent(editText: EditText) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_format_text_size, null)
        val defaultSize = dialogView.findViewById<Button>(R.id.defaultSizeBtn)
        val textSize = dialogView.findViewById<SeekBar>(R.id.sbTextSize)
        val textPreview = dialogView.findViewById<TextView>(R.id.textPreview)
        textPreview.text = "Text size $selectedTextSize"
        defaultSize.setOnClickListener {
            selectedTextSize = 18
            textPreview.text = "Text size $selectedTextSize"
            textSize.progress = 18
        }
        textSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                textPreview.text = "Text size $p1"
                textPreview.textSize = p1.toFloat()
                selectedTextSize = p1
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton("OK"){dialog, _ ->
                changeTextSizeForContent(editText, selectedTextSize)
                dialog.dismiss()
            }
        builder.create().show()
    }
//    private fun showFormatTextSizeDialogForTitle(editText: EditText) {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_format_text_size, null)
//        val defaultSize = dialogView.findViewById<Button>(R.id.defaultSizeBtn)
//        val textSize = dialogView.findViewById<SeekBar>(R.id.sbTextSize)
//        val textPreview = dialogView.findViewById<TextView>(R.id.textPreview)
//        textPreview.text = "Text size $selectedTextSize"
//        defaultSize.setOnClickListener {
//            selectedTextSize = 18
//            textPreview.text = "Text size $selectedTextSize"
//            textSize.progress = 18
//        }
//        textSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
//            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
//                textPreview.text = "Text size $p1"
//                textPreview.textSize = p1.toFloat()
//                selectedTextSize = p1
//            }
//
//            override fun onStartTrackingTouch(p0: SeekBar?) {
//
//            }
//
//            override fun onStopTrackingTouch(p0: SeekBar?) {
//
//            }
//        })
//        val builder = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setNegativeButton("Cancel"){dialog, _ ->
//                dialog.dismiss()
//            }.setPositiveButton("OK"){dialog, _ ->
//                changeTextSizeForTitle(editText, selectedTextSize)
//                dialog.dismiss()
//            }
//        builder.create().show()
//    }

    private fun changeTextSizeForContent(editText: EditText, size: Int) {
        onTextSizeSelected(size)
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd
        if(start < end){
            val spannable = editText.text as Spannable
            spannable.setSpan(
                AbsoluteSizeSpan(size, true),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
//    private fun changeTextSizeForTitle(editText: EditText, size: Int) {
//        onTextSizeSelected(size)
//        val start = binding.edtTitle.selectionStart
//        val end = binding.edtTitle.selectionEnd
//        if(start < end){
//            val spannable = editText.text as Spannable
//            spannable.setSpan(
//                AbsoluteSizeSpan(size, true),
//                start,
//                end,
//                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//        }
//    }

    private fun applyUnderline() {
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd
        if(start < end){
            val spannable = SpannableStringBuilder(binding.edtContent.text)
            spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.edtContent.text = spannable
            binding.edtContent.setSelection(start, end)
            val spans = (binding.edtContent.text as SpannableStringBuilder).getSpans(start, end, UnderlineSpan::class.java)
        }
    }

    private fun applyStyle(style: Int, editText: EditText, applyToAll: Boolean = false) {
//        val text = editText.text
//        if(text is Spannable){
//            var start = editText.selectionStart
//            var end = editText.selectionEnd
//            var minStart = editText.text.length
//            var maxEnd = 0
//
//            Log.d("span", "selection start: $start")
//            Log.d("span", "selection end: $end")
//            val styleSpans = text.getSpans(start, end, StyleSpan::class.java)
//            var styleExists = false
//            for (span in styleSpans){
//                minStart = min(minStart, text.getSpanStart(span))
//                maxEnd = max(maxEnd, text.getSpanEnd(span))
//                Log.d("span", "minStart: $minStart")
//                Log.d("span", "maxEnd: $maxEnd")
//                if(span.style == style){
//                    text.removeSpan(span)
//                    styleExists = true
//                }
//            }
//            if(start > minStart) start = minStart
//            if(end < maxEnd) end = maxEnd
//
//            Log.d("span", "start: $start")
//            Log.d("span", "end: $end")
//            if(!styleExists){
//                text.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//            }
//        }
        val text = editText.text
        if (text is Spannable) {
            val start = editText.selectionStart
            val end = editText.selectionEnd
            val styleSpans = text.getSpans(start, end, StyleSpan::class.java)
            var styleExists = false
            for (span in styleSpans) {
                if (span.style == style) {
                    text.removeSpan(span)
                    styleExists = true
                }
            }

            if (!styleExists) {
                text.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
    private fun applyStyleToRange(style: Int, editText: EditText, start: Int, end: Int) {
        val text = editText.text
        if (text is Spannable) {
            text.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun strikeThrough(editText: EditText) {
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd
        if(start < end){
            val spannable = editText.text as Spannable
            val spans = spannable.getSpans(start, end, StrikethroughSpan::class.java)
            if(spans.isNotEmpty()){
                for (span in spans){
                    spannable.setSpan(
                        StrikethroughSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }else{
                spannable.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun parseColor(colorString: String): Int{
        return Color.parseColor(colorString)
    }

    override fun onColorClick(color: String) {
        selectedColor = color
        selectedFormatTextColor = color
        if(colorFillOrText == 0){
            selectColorTv.setBackgroundColor(Color.parseColor(color))
        }else if( colorFillOrText == 1){
            selectColorTv.setTextColor(Color.parseColor(color))
        }
    }

    override fun onColorClick(color: String, imageView: ImageView) {
    }

    private fun applyTextColor(start: Int, end: Int){
        val text = binding.edtContent.text
        if(text is Spannable && currentTextColor != null){
            text.setSpan(ForegroundColorSpan(currentTextColor!!), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyBackgroundColor(start: Int, end: Int){
        val text = binding.edtContent.text
        if(text is Spannable && currentBackgroundColor != null){
            text.getSpans(start, end, BackgroundColorSpan::class.java).forEach { span ->
                text.removeSpan(span)
            }
            text.setSpan(BackgroundColorSpan(currentBackgroundColor!!), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyTextSize(start: Int, end: Int){
        val text = binding.edtContent.text
        if(text is Spannable && currentTextSize != null){
            text.setSpan(AbsoluteSizeSpan(currentTextSize!!.toInt(), true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    private fun applyTextColorForTitle(start: Int, end: Int){
        val text = binding.edtTitle.text
        if(text is Spannable && currentTextColor != null){
            text.setSpan(ForegroundColorSpan(currentTextColor!!), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyBackgroundColorForTitle(start: Int, end: Int){
        val text = binding.edtTitle.text
        if(text is Spannable && currentBackgroundColor != null){
            text.setSpan(BackgroundColorSpan(currentBackgroundColor!!), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun applyTextSizeForTitle(start: Int, end: Int){
        val text = binding.edtTitle.text
        if(text is Spannable && currentTextSize != null){
            text.setSpan(AbsoluteSizeSpan(currentTextSize!!.toInt(), true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    private fun onTextColorSelected(color: Int){
        currentTextColor = color
        applyTextColor(binding.edtContent.selectionStart, binding.edtContent.selectionEnd)
    }

    private fun onBackgroundColorSelected(color: Int){
        currentBackgroundColor = color
        applyBackgroundColor(binding.edtContent.selectionStart, binding.edtContent.selectionEnd)
    }

    private fun onTextSizeSelected(size: Int){
        currentTextSize = size
        applyTextSize(binding.edtContent.selectionStart, binding.edtContent.selectionEnd)
    }
    private fun onTextColorSelectedForTitle(color: Int){
        currentTextColor = color
        applyTextColor(binding.edtTitle.selectionStart, binding.edtTitle.selectionEnd)
    }

    private fun onBackgroundColorSelectedForTitle(color: Int){
        currentBackgroundColor = color
        applyBackgroundColor(binding.edtTitle.selectionStart, binding.edtTitle.selectionEnd)
    }

    private fun onTextSizeSelectedForTitle(size: Int){
        currentTextSize = size
        applyTextSize(binding.edtTitle.selectionStart, binding.edtTitle.selectionEnd)
    }

    private fun startSelectionMonitor() {
        handler.post(object : Runnable {
            override fun run() {
                val start = binding.edtContent.selectionStart
                val end = binding.edtContent.selectionEnd

                // Chỉ kiểm tra khi vùng chọn thay đổi
                if (start != previousStart || end != previousEnd) {
                    binding.bold.setBackgroundColor(Color.TRANSPARENT)
                    binding.italic.setBackgroundColor(Color.TRANSPARENT)
                    binding.underline.setBackgroundColor(Color.TRANSPARENT)
                    binding.strikeThrough.setBackgroundColor(Color.TRANSPARENT)
//                    binding.fillColor.setBackgroundColor(Color.TRANSPARENT)
//                    binding.textColor.setBackgroundColor(Color.TRANSPARENT)

                    if(isSingleEffectAppliedOnly(binding.edtContent, start, end)){
                        checkTextStyle(start, end)
                        previousStart = start
                        previousEnd = end
                    }
                }

                // Kiểm tra lại sau mỗi 100ms
                handler.postDelayed(this, 100)
            }
        })
    }
    private fun checkTextStyle(start: Int, end: Int) {
        if (start < end) {
            val spannable = binding.edtContent.text as Spannable

            // Kiểm tra in đậm
            val boldSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
            val isBoldOnly = boldSpans.isNotEmpty() && boldSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

            // Kiểm tra in nghiêng
            val italicSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
            val isItalicOnly = italicSpans.isNotEmpty() && italicSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

            // Kiểm tra gạch chân (underline)
            val underlineSpans = spannable.getSpans(start, end, UnderlineSpan::class.java)
            val isUnderlineOnly = underlineSpans.isNotEmpty() && underlineSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

            // Kiểm tra gạch ngang (strikethrough)
            val strikeSpans = spannable.getSpans(start, end, StrikethroughSpan::class.java)
            val isStrikethroughOnly = strikeSpans.isNotEmpty() && strikeSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

            // Kiểm tra màu nền (fill color)
            val backgroundColorSpans = spannable.getSpans(start, end, BackgroundColorSpan::class.java)
            val isBackgroundOnly = backgroundColorSpans.isNotEmpty() && backgroundColorSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

            // Kiểm tra màu chữ (text color)
            val colorSpans = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
            val isColorOnly = colorSpans.isNotEmpty() && colorSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

            // Kiểm tra kích thước chữ (text size)
            val sizeSpans = spannable.getSpans(start, end, AbsoluteSizeSpan::class.java)
            val isSizeOnly = sizeSpans.isNotEmpty() && sizeSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

            when {
                isBoldOnly -> binding.bold.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                isItalicOnly -> binding.italic.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
//                isColorOnly -> binding.fillColor.setBackgroundColor(currentBackgroundColor!!)
//                isBackgroundOnly -> binding.textColor.setBackgroundColor(currentTextColor!!)
                isUnderlineOnly -> binding.underline.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                isStrikethroughOnly -> binding.strikeThrough.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                isSizeOnly -> binding.textSize.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                else -> println("Vùng chọn có nhiều định dạng hoặc không có định dạng nào.")
            }
        }
    }
    fun isSingleEffectAppliedOnly(editText: EditText, start: Int, end: Int): Boolean {
        val spannable = editText.text as Spannable

        // Kiểm tra toàn bộ vùng chọn chỉ có kiểu in đậm
        val boldSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
        val isBoldOnly = boldSpans.isNotEmpty() && boldSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

        // Kiểm tra toàn bộ vùng chọn chỉ có kiểu in nghiêng
        val italicSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
        val isItalicOnly = italicSpans.isNotEmpty() && italicSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

        // Kiểm tra toàn bộ vùng chọn chỉ có màu chữ (text color)
        val colorSpans = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
        val isColorOnly = colorSpans.isNotEmpty() && colorSpans.size == 1 &&
                spannable.getSpanStart(colorSpans[0]) <= start && spannable.getSpanEnd(colorSpans[0]) >= end

        // Kiểm tra toàn bộ vùng chọn chỉ có màu nền (fill color)
        val backgroundColorSpans = spannable.getSpans(start, end, BackgroundColorSpan::class.java)
        val isBackgroundOnly = backgroundColorSpans.isNotEmpty() && backgroundColorSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

        // Kiểm tra toàn bộ vùng chọn chỉ có kiểu gạch chân (underline)
        val underlineSpans = spannable.getSpans(start, end, UnderlineSpan::class.java)
        val isUnderlineOnly = underlineSpans.isNotEmpty() && underlineSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

        // Kiểm tra toàn bộ vùng chọn chỉ có kiểu gạch ngang (strikethrough)
        val strikeSpans = spannable.getSpans(start, end, StrikethroughSpan::class.java)
        val isStrikethroughOnly = strikeSpans.isNotEmpty() && strikeSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

        // Kiểm tra toàn bộ vùng chọn chỉ có kích thước chữ duy nhất (text size)
        val sizeSpans = spannable.getSpans(start, end, AbsoluteSizeSpan::class.java)
        val isSizeOnly = sizeSpans.isNotEmpty() && sizeSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }

        // Trả về true nếu chỉ có một loại hiệu ứng duy nhất áp dụng cho toàn bộ vùng chọn
        return listOf(isBoldOnly, isItalicOnly, isColorOnly, isBackgroundOnly, isUnderlineOnly, isStrikethroughOnly, isSizeOnly).count { it } == 1
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Ngừng kiểm tra khi Activity bị hủy
    }

    fun onFillColorSelected(color: Int) {
        currentBackgroundColor = color
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd

        // Gọi applyBackgroundColor ngay lập tức để áp dụng màu nền
        if (start != end) {  // Chỉ áp dụng khi có vùng chọn
            applyBackgroundColor(start, end)
        }
    }
}