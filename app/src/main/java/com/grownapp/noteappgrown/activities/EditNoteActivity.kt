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
import androidx.core.text.HtmlCompat
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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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

        startSelectionMonitor()
    }

    private fun toggleStyleForContent(style: String){
        styleStates[style] = !(styleStates[style]?:false)
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd
        applyCurrentStylesForContent(start, end)
    }

    private fun applyCurrentStylesForContent(start: Int, end: Int) {
        val text = binding.edtContent.text
        if (text is Spannable) {
            // Lặp qua tất cả các vị trí trong vùng chọn
            for (i in start until end) {
                if (!text[i].isWhitespace()) {
                    // Xóa hiệu ứng hiện tại nếu đã có
                    val boldSpans = text.getSpans(i, i + 1, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
                    if (styleStates["BOLD"] == true) {
                        if (boldSpans.isEmpty()) {
                            text.setSpan(StyleSpan(Typeface.BOLD), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        binding.bold.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                    } else {
                        boldSpans.forEach { text.removeSpan(it) }
                    }

                    val italicSpans = text.getSpans(i, i + 1, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
                    if (styleStates["ITALIC"] == true) {
                        if (italicSpans.isEmpty()) {
                            text.setSpan(StyleSpan(Typeface.ITALIC), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        binding.italic.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                    } else {
                        italicSpans.forEach { text.removeSpan(it) }
                    }

                    val underlineSpans = text.getSpans(i, i + 1, UnderlineSpan::class.java)
                    if (styleStates["UNDERLINE"] == true) {
                        if (underlineSpans.isEmpty()) {
                            text.setSpan(UnderlineSpan(), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        binding.underline.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                    } else {
                        underlineSpans.forEach { text.removeSpan(it) }
                    }

                    val strikethroughSpans = text.getSpans(i, i + 1, StrikethroughSpan::class.java)
                    if (styleStates["STRIKETHROUGH"] == true) {
                        if (strikethroughSpans.isEmpty()) {
                            text.setSpan(StrikethroughSpan(), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        binding.strikeThrough.setBackgroundColor(ContextCompat.getColor(this, R.color.enable))
                    } else {
                        strikethroughSpans.forEach { text.removeSpan(it) }
                    }

                    val textColorSpans = text.getSpans(i, i + 1, ForegroundColorSpan::class.java)
                    if (styleStates["TEXTCOLOR"] == true && currentTextColor != null) {
                        if (textColorSpans.isEmpty() || textColorSpans.none { it.foregroundColor == currentTextColor }) {
                            text.setSpan(ForegroundColorSpan(currentTextColor!!), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        textColorSpans.forEach { text.removeSpan(it) }
                    }

                    val backgroundColorSpans = text.getSpans(i, i + 1, BackgroundColorSpan::class.java)
                    if (styleStates["FILLCOLOR"] == true && currentBackgroundColor != null) {
                        if (backgroundColorSpans.isEmpty() || backgroundColorSpans.none { it.backgroundColor == currentBackgroundColor }) {
                            text.setSpan(BackgroundColorSpan(currentBackgroundColor!!), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        backgroundColorSpans.forEach { text.removeSpan(it) }
                    }
                }
            }
        }
    }


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
        while (textUndo.isNotEmpty()){
            isUndo = true
            val (previousText, previousCursorPosition) = textUndo.removeLast()
            textRedo.add(Pair(previousText, previousCursorPosition))
            binding.edtContent.setText(previousText)
            binding.edtContent.setSelection(previousCursorPosition)
        }

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
        val spannableText = binding.edtContent.text as SpannableStringBuilder
        val noteContent = serializeSpannableString(spannableText)
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
    fun serializeSpannableString(spannable: SpannableStringBuilder): String {
        val jsonObject = JSONObject()
        jsonObject.put("text", spannable.toString()) // Lưu nội dung văn bản

        val spansArray = JSONArray()

        val spans = spannable.getSpans(0, spannable.length, Any::class.java)
        for (span in spans) {
            val spanObject = JSONObject()
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)

            when (span) {
                is ForegroundColorSpan -> {
                    spanObject.put("type", "ForegroundColorSpan")
                    spanObject.put("color", span.foregroundColor)
                }
                is BackgroundColorSpan -> {
                    spanObject.put("type", "BackgroundColorSpan")
                    spanObject.put("color", span.backgroundColor)
                }
                is StyleSpan -> {
                    spanObject.put("type", "StyleSpan")
                    spanObject.put("style", span.style)
                }
                is AbsoluteSizeSpan -> {
                    spanObject.put("type", "AbsoluteSizeSpan")
                    spanObject.put("size", span.size)
                    spanObject.put("dip", span.dip)
                }
                is UnderlineSpan -> {
                    spanObject.put("type", "UnderlineSpan")
                }
                is StrikethroughSpan -> {
                    spanObject.put("type", "StrikethroughSpan")
                }

                else -> {
                    continue
                }
            }

            spanObject.put("start", start)
            spanObject.put("end", end)

            spansArray.put(spanObject)
        }

        jsonObject.put("spans", spansArray)

        return jsonObject.toString()
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
            intent.getStringExtra("content").orEmpty()
        }else{
            noteViewModel.getNoteById(id).content
        }
        println(content)
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
        if (content.isNotEmpty()) {
            try {
                val spannableContent = deserializeSpannableString(content)
                binding.edtContent.setText(spannableContent)
            } catch (e: JSONException) {
                e.printStackTrace()
                binding.edtContent.setText(content)
            }
        } else {
            binding.edtContent.setText("")
        }
    }
    fun deserializeSpannableString(serializedText: String): SpannableStringBuilder {
        val jsonObject = JSONObject(serializedText)
        val text = jsonObject.getString("text")
        val spannable = SpannableStringBuilder(text)

        val spansArray = jsonObject.getJSONArray("spans")
        for (i in 0 until spansArray.length()) {
            val spanObject = spansArray.getJSONObject(i)
            val start = spanObject.getInt("start")
            val end = spanObject.getInt("end")

            when (spanObject.getString("type")) {
                "ForegroundColorSpan" -> {
                    val color = spanObject.getInt("color")
                    spannable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "BackgroundColorSpan" -> {
                    val color = spanObject.getInt("color")
                    spannable.setSpan(BackgroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "StyleSpan" -> {
                    val style = spanObject.getInt("style")
                    spannable.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "AbsoluteSizeSpan" -> {
                    val size = spanObject.getInt("size")
                    val dip = spanObject.getBoolean("dip")
                    spannable.setSpan(AbsoluteSizeSpan(size, dip), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "UnderlineSpan" -> {
                    spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                "StrikethroughSpan" -> {
                    spannable.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        return spannable
    }

    private fun formattingBarAction() {
        binding.bold.setOnClickListener {
            toggleStyleForContent("BOLD")

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


            }else{
                changeColorSelected(editText, color, imageView)

            }
        }
    }

    private fun applyBackgroundColorToSelection(editText: EditText, color: Int) {
        val spannable = editText.text as Spannable
        val start = editText.selectionStart
        val end = editText.selectionEnd


        spannable.getSpans(start, end, BackgroundColorSpan::class.java).forEach { span ->
            spannable.removeSpan(span)
        }


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

    private fun applyTextColor(start: Int, end: Int) {
        val text = binding.edtContent.text
        if (text is Spannable && currentTextColor != null) {

            text.getSpans(start, end, ForegroundColorSpan::class.java).forEach { span ->
                text.removeSpan(span)
            }


            for (i in start until end) {
                if (!text[i].isWhitespace()) {
                    text.setSpan(ForegroundColorSpan(currentTextColor!!), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun applyBackgroundColor(start: Int, end: Int) {
        val text = binding.edtContent.text
        if (text is Spannable && currentBackgroundColor != null) {

            text.getSpans(start, end, BackgroundColorSpan::class.java).forEach { span ->
                text.removeSpan(span)
            }

            for (i in start until end) {
                if (!text[i].isWhitespace()) {
                    text.setSpan(BackgroundColorSpan(currentBackgroundColor!!), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    private fun applyTextSize(start: Int, end: Int) {
        val text = binding.edtContent.text
        if (text is Spannable && currentTextSize != null) {

            // Xóa các `AbsoluteSizeSpan` hiện tại trong phạm vi
            text.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach { span ->
                text.removeSpan(span)
            }

            // Áp dụng `AbsoluteSizeSpan` cho toàn bộ phạm vi
            text.setSpan(AbsoluteSizeSpan(currentTextSize!!.toInt(), true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }


//    private fun applyTextSize(start: Int, end: Int) {
//        val text = binding.edtContent.text
//        if (text is Spannable && currentTextSize != null) {
//
//            text.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach { span ->
//                text.removeSpan(span)
//            }
//
//            for (i in start until end) {
//                if (!text[i].isWhitespace()) {
//                    text.setSpan(AbsoluteSizeSpan(currentTextSize!!.toInt(), true), i, i + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//                }
//            }
//        }
//    }

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


                if (start != previousStart || end != previousEnd) {
                    binding.bold.setBackgroundColor(Color.TRANSPARENT)
                    binding.italic.setBackgroundColor(Color.TRANSPARENT)
                    binding.underline.setBackgroundColor(Color.TRANSPARENT)
                    binding.strikeThrough.setBackgroundColor(Color.TRANSPARENT)


                    if(isSingleEffectAppliedOnly(binding.edtContent, start, end)){
                        checkTextStyle(start, end)
                        previousStart = start
                        previousEnd = end
                    }
                }

                handler.postDelayed(this, 100)
            }
        })
    }
    private fun checkTextStyle(start: Int, end: Int) {
        if (start < end) {
            val spannable = binding.edtContent.text as Spannable


            val boldSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
            val isBoldOnly = boldSpans.isNotEmpty() && boldSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


            val italicSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
            val isItalicOnly = italicSpans.isNotEmpty() && italicSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


            val underlineSpans = spannable.getSpans(start, end, UnderlineSpan::class.java)
            val isUnderlineOnly = underlineSpans.isNotEmpty() && underlineSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


            val strikeSpans = spannable.getSpans(start, end, StrikethroughSpan::class.java)
            val isStrikethroughOnly = strikeSpans.isNotEmpty() && strikeSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


            val backgroundColorSpans = spannable.getSpans(start, end, BackgroundColorSpan::class.java)
            val isBackgroundOnly = backgroundColorSpans.isNotEmpty() && backgroundColorSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


            val colorSpans = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
            val isColorOnly = colorSpans.isNotEmpty() && colorSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


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


        val boldSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
        val isBoldOnly = boldSpans.isNotEmpty() && boldSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


        val italicSpans = spannable.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
        val isItalicOnly = italicSpans.isNotEmpty() && italicSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


        val colorSpans = spannable.getSpans(start, end, ForegroundColorSpan::class.java)
        val isColorOnly = colorSpans.isNotEmpty() && colorSpans.size == 1 &&
                spannable.getSpanStart(colorSpans[0]) <= start && spannable.getSpanEnd(colorSpans[0]) >= end


        val backgroundColorSpans = spannable.getSpans(start, end, BackgroundColorSpan::class.java)
        val isBackgroundOnly = backgroundColorSpans.isNotEmpty() && backgroundColorSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


        val underlineSpans = spannable.getSpans(start, end, UnderlineSpan::class.java)
        val isUnderlineOnly = underlineSpans.isNotEmpty() && underlineSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


        val strikeSpans = spannable.getSpans(start, end, StrikethroughSpan::class.java)
        val isStrikethroughOnly = strikeSpans.isNotEmpty() && strikeSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


        val sizeSpans = spannable.getSpans(start, end, AbsoluteSizeSpan::class.java)
        val isSizeOnly = sizeSpans.isNotEmpty() && sizeSpans.all { spannable.getSpanStart(it) <= start && spannable.getSpanEnd(it) >= end }


        return listOf(isBoldOnly, isItalicOnly, isColorOnly, isBackgroundOnly, isUnderlineOnly, isStrikethroughOnly, isSizeOnly).count { it } == 1
    }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    fun onFillColorSelected(color: Int) {
        currentBackgroundColor = color
        val start = binding.edtContent.selectionStart
        val end = binding.edtContent.selectionEnd


        if (start != end) {
            applyBackgroundColor(start, end)
        }
    }
}