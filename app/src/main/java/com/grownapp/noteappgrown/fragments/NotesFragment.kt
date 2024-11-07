package com.grownapp.noteappgrown.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.activities.EditNoteActivity
import com.grownapp.noteappgrown.activities.MainActivity
import com.grownapp.noteappgrown.adapter.ListCategoryAdapter
import com.grownapp.noteappgrown.adapter.ListColorAdapter
import com.grownapp.noteappgrown.adapter.ListNoteAdapter
import com.grownapp.noteappgrown.databinding.FragmentNotesBinding
import com.grownapp.noteappgrown.listeners.OnColorClickListener
import com.grownapp.noteappgrown.listeners.OnItemClickListener
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.models.Note
import com.grownapp.noteappgrown.models.NoteCategoryCrossRef
import com.grownapp.noteappgrown.viewmodel.CategoryViewModel
import com.grownapp.noteappgrown.viewmodel.NoteCategoryViewModel
import com.grownapp.noteappgrown.viewmodel.NoteViewModel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

class NotesFragment : Fragment(R.layout.fragment_notes), MenuProvider, androidx.appcompat.widget.SearchView.OnQueryTextListener, OnColorClickListener {
    private val binding: FragmentNotesBinding by lazy {
        FragmentNotesBinding.inflate(layoutInflater)
    }
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var noteCategoryViewModel: NoteCategoryViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var noteAdapter: ListNoteAdapter
    private lateinit var categoryAdapter: ListCategoryAdapter
    private lateinit var colorAdapter: ListColorAdapter
    private lateinit var currentList: List<Note>
    private lateinit var noteView: View
    private var isAlternateMenuVisible: Boolean = false
    private lateinit var categories: List<Category>
    private var selectedColor: String? = null
    private var optionSort: Int = -1
    private val colors = listOf(
        "#FFCDD2", "#F8BBD0", "#E1BEE7", "#D1C4E9", "#C5CAE9",
        "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB", "#C8E6C9",
        "#DCEDC8", "#F0F4C3", "#FFECB3", "#FFE0B2", "#FFCCBC",
        "#D7CCC8", "#F5F5F5", "#CFD8DC", "#FF8A80", "#FF80AB"
    )
    companion object {
        private const val READ_FILE_REQUEST_CODE = 101
        private const val REQUEST_WRITE_PERMISSION = 1001
        private const val REQUEST_CODE_PICK_DIRECTORY = 1002
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        setHasOptionsMenu(true)
        noteViewModel = (activity as MainActivity).noteViewModel
        categoryViewModel = (activity as MainActivity).categoryViewModel
        noteCategoryViewModel = (activity as MainActivity).noteCategoryViewModel
        noteView = view
        setupNoteRecyclerView()
        binding.addNoteFab.setOnClickListener {
            addNote()
        }
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
            toolbar.setTitle("Notepad Free")
            toolbar.overflowIcon?.setTint(Color.WHITE)
        }
    }

    private fun addNote() {
        val note = Note(0, "", "",getCurrentTime(), getCurrentTime(), null, false)
        noteViewModel.addNote(note)
        val intent = Intent(requireContext(), EditNoteActivity::class.java)
        intent.putExtra("id", note.id)
        intent.putExtra("title", note.title)
        intent.putExtra("content", note.content)
        intent.putExtra("created", note.created)
        intent.putExtra("time", note.time)
        intent.putExtra("color", note.color)
        startActivity(intent)
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val formattedDate = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).format(calendar.time)
        return formattedDate
    }

    private fun setupNoteRecyclerView() {
        noteAdapter = ListNoteAdapter(requireContext(), object : OnItemClickListener{
            override fun onNoteClick(note: Note, isChoose: Boolean) {
                if(!isChoose && !isAlternateMenuVisible){
                    val intent = Intent(context, EditNoteActivity::class.java)
                    intent.putExtra("id", note.id)
                    intent.putExtra("title", note.title)
                    intent.putExtra("content",note.content)
                    intent.putExtra("created", note.time)
                    intent.putExtra("color", note.color)
                    intent.putExtra("sort", optionSort)
                    startActivity(intent)
                }
                if(isAlternateMenuVisible){
                    updateSelectedCount()
                }
            }

            override fun onNoteLongClick(note: Note) {

                isAlternateMenuVisible = true
                requireActivity().invalidateOptionsMenu()
                if(isAlternateMenuVisible){
                    changeBackNavigationIcon()
                    updateSelectedCount()
                }
            }
        })
        categoryAdapter = ListCategoryAdapter(requireContext())
        binding.listNoteRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.listNoteRecyclerView.adapter = noteAdapter
        activity?.let {
            if(optionSort == -1){
                noteViewModel.getAllNotes().observe(viewLifecycleOwner){note ->
                    noteAdapter.updateNotes(note)
                    currentList = note
                    updateUI(note)
                }
            }
        }
        activity?.let {
            categoryViewModel.getAllCategory().observe(viewLifecycleOwner){ category ->
                categoryAdapter.differ.submitList(category)
                categories = categoryAdapter.differ.currentList
            }
        }
    }

    private fun updateUI(note: List<Note>) {
        if(note.isNotEmpty()){
            binding.listNoteRecyclerView.visibility = View.VISIBLE
        }else{
            binding.listNoteRecyclerView.visibility = View.GONE
        }
    }

    private fun changeBackNavigationIcon() {
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
            val drawerLayout = mainActivity.findViewById<DrawerLayout>(R.id.drawerLayout)
            toolbar.setNavigationIcon(R.drawable.ic_back_24)
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
            toolbar.setNavigationOnClickListener {
                noteAdapter.isChoosing = false
                activity?.invalidateOptionsMenu()
                isAlternateMenuVisible = !isAlternateMenuVisible
                clearSelection()
                toolbar.setNavigationIcon(R.drawable.ic_menu_24)
                toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
                toolbar.setNavigationOnClickListener {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                toolbar.setTitle(requireContext().getString(R.string.notepadFree))
            }
        }
    }

    private fun clearSelection() {
        noteAdapter.clearSelection()
    }

    private fun updateSelectedCount() {
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
            if(isAlternateMenuVisible){
                toolbar.setTitle(noteAdapter.getSelectedItemsCount().toString())
            }else{
                toolbar.setTitle(requireContext().getString(R.string.notepadFree))
                noteAdapter.clearSelection()
            }
        }
    }

    private fun changeDrawerNavigationIcon(){
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
            val drawerLayout = mainActivity.findViewById<DrawerLayout>(R.id.drawerLayout)
            noteAdapter.isChoosing = false
            isAlternateMenuVisible = false
            activity?.invalidateOptionsMenu()
            clearSelection()
            toolbar.setNavigationIcon(R.drawable.ic_menu_24)
            toolbar.navigationIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.white))
            toolbar.setNavigationOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            toolbar.setTitle(requireContext().getString(R.string.notepadFree))
        }
    }

    private fun showCategorizeDialog(){
        val checkedItem = BooleanArray(categories.size)
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Select category").setPositiveButton("OK"){dialog, which ->
            val selectedCategories = mutableListOf<Category>()
            val unSelectedCategories = mutableListOf<Category>()
            for(i in categories.indices){
                if(checkedItem[i]){
                    selectedCategories.add(categories[i])
                }else{
                    unSelectedCategories.add(categories[i])
                }
            }
            val noteCategoryCrossRefs = mutableListOf<NoteCategoryCrossRef>()
            val selectedNotes = noteAdapter.getSelectedItems()
            for(noteId in selectedNotes.map { it.id }){
                noteCategoryViewModel.deleteNoteCategoryCrossRefs(noteId)
                for(categoryId in selectedCategories.map { it.id }){
                    noteCategoryCrossRefs.add(NoteCategoryCrossRef(noteId, categoryId))
                }
            }
            noteCategoryViewModel.addListNoteCategory(noteCategoryCrossRefs)
            isAlternateMenuVisible = !isAlternateMenuVisible
            changeDrawerNavigationIcon()
            requireActivity().invalidateOptionsMenu()
            updateSelectedCount()
            dialog.dismiss()
        }.setNegativeButton("Cancel"){dialog, which ->
            dialog.dismiss()
        }.setMultiChoiceItems(categories.map { it.categoryName }.toTypedArray(), checkedItem){dialog, which, isChecked ->
            checkedItem[which] = isChecked
        }
        builder.create().show()
    }

    private fun showDeleteDialog(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete").setMessage("Delete the selected note?").setPositiveButton("Delete"){dialog, which ->
            deleteSelectedItem()
            isAlternateMenuVisible = false
            changeDrawerNavigationIcon()
            requireActivity().invalidateOptionsMenu()
            updateSelectedCount()
            dialog.dismiss()
        }.setNegativeButton("Cancel"){dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun deleteSelectedItem() {
        val selectedNotes = noteAdapter.getSelectedItems()
        val selectedIds = selectedNotes.map { it.id }
        noteViewModel.moveToTrash(selectedIds)
        noteAdapter.removeSelectedItems()
    }

    private fun showOptionDialog(){
        val sortOption = arrayOf("edit date: from newest", "edit date: from oldest", "title: A to Z", "title: Z to A", "creation date: from newest", "creation date: from oldest", "color: in order as shown on color palette")
        var selectedOption = 0
        val noteList = currentList
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Sort by").setPositiveButton("Sort"){dialog, which ->
            when (selectedOption){
                0 -> {
                    optionSort = 0
                    sortByEditDateNewest(noteList)
                }
                1 -> {
                    optionSort = 1
                    sortByEditDateOldest(noteList)
                }
                2 -> {
                    optionSort = 2
                    sortByTitleAToZ(noteList)
                }
                3 -> {
                    optionSort = 3
                    sortByTitleZToA(noteList)
                }
                4 -> {
                    optionSort = 4
                    sortByCreationDateNewest(noteList)
                }
                5 -> {
                    optionSort = 5
                    sortByCreationDateOldest(noteList)
                }
            }
        }.setNegativeButton("Cancel"){dialog, which ->
            dialog.dismiss()
        }.setSingleChoiceItems(sortOption, selectedOption){dialog, which ->
            selectedOption = which
            if(selectedOption == 4 || selectedOption == 5){
                noteAdapter.isCreated = true
            }else{
                noteAdapter.isCreated = true
            }
        }
        builder.create().show()
    }

    private fun sortByCreationDateOldest(noteList: List<Note>) {
        noteAdapter.updateNotes(noteList.sortedBy { it.id })
        noteAdapter.notifyDataSetChanged()
    }

    private fun sortByCreationDateNewest(noteList: List<Note>) {
        noteAdapter.updateNotes(noteList.sortedByDescending { it.id })
        noteAdapter.notifyDataSetChanged()
    }

    private fun sortByTitleZToA(noteList: List<Note>) {
        noteAdapter.updateNotes(noteList.sortedByDescending { it.title })
        noteAdapter.notifyDataSetChanged()
    }

    private fun sortByTitleAToZ(noteList: List<Note>) {
        noteAdapter.updateNotes(currentList.sortedBy { it.title })
        noteAdapter.notifyDataSetChanged()
    }

    private fun sortByEditDateOldest(noteList: List<Note>) {
        noteAdapter.updateNotes(noteList.sortedBy { it.time })
        noteAdapter.notifyDataSetChanged()
    }

    private fun sortByEditDateNewest(noteList: List<Note>) {
        noteAdapter.updateNotes(noteList.sortedByDescending { it.time })
        noteAdapter.notifyDataSetChanged()
    }

    private fun searchNote(query: String?){
        if(query != null){
            if(query.isEmpty()){
                noteAdapter.updateNotes(currentList)
            }else{
                noteViewModel.searchNote("%$query%").observe(this){notes ->
                    noteAdapter.updateNotes(notes)
                }
            }
        }
    }
    private fun requestWritePermission(){
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_PERMISSION)
        }else{
            selectDirectory()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_WRITE_PERMISSION){
            if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                selectDirectory()
            }else{
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_PICK_DIRECTORY)
    }

    private fun exportNoteToTextFile(uri: Uri){
        var selectedNotes = noteAdapter.getSelectedItems()
        if(selectedNotes.isEmpty()){
            selectedNotes = noteAdapter.getSelectedItems()
        }
        selectedNotes.forEach { note ->
            val fileName = "${note.title}.txt"
            createFile(uri, fileName, note.content)
        }
        Toast.makeText(requireContext(), "${selectedNotes.size} note(s) exported", Toast.LENGTH_SHORT).show()
    }

    private fun createFile(uri: Uri, fileName: String, content: String) {
        try {
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )
            val docUri = DocumentsContract.createDocument(requireContext().contentResolver, documentUri, "text/plain", fileName)
            docUri?.let { requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            } }
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_PICK_DIRECTORY && resultCode == Activity.RESULT_OK){
            val uri = data?.data
            uri?.let {
                exportNoteToTextFile(it)
            }
        }
        if(resultCode == READ_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val selectedFile = mutableListOf<Uri>()
            data?.clipData?.let { clipData ->
                for(i in 0 until clipData.itemCount){
                    val uri = clipData.getItemAt(i).uri
                    selectedFile.add(uri)
                }
            }?:run {
                data?.data?.let { uri ->
                    Log.d("TAG", "onActivityResult: $uri")
                    selectedFile.add(uri)
                }
            }
            handleSelectedFiles(selectedFile)
            Toast.makeText(context, "${selectedFile.size} note(s) added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        for (uri in uris){
            val note = createNoteFromTextFile(uri)
            noteViewModel.addNote(note)
        }
    }

    private fun createNoteFromTextFile(uri: Uri): Note {
        val content = readTextTxt(uri)
        val title = getFileName(uri)
        val note = Note(0, title!!, content, getCurrentTime(), getCurrentTime(), null, false)
        return note
    }

    private fun readTextTxt(uri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        try {
            while (reader.readLine().also { line = it } != null){
                stringBuilder.append(line).append("\n")
            }
        }catch (e: IOException){
            e.printStackTrace()
        }finally {
            try {
                inputStream?.close()
            }catch (e: IOException){
                e.printStackTrace()
            }
        }
        return stringBuilder.toString()
    }

    private fun getFileName(uri: Uri): String? {
        val contentResolver = requireContext().contentResolver
        var fileTxtName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if(it.moveToFirst()){
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val fileName = if(displayNameIndex != -1) it.getString(displayNameIndex) else "Unknown"
                fileTxtName = fileName
            }
        }
        return fileTxtName
    }

    private fun openTextFile(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, READ_FILE_REQUEST_CODE)
    }
    private fun showColorPickerDialog(){
        val dialogView = layoutInflater.inflate(R.layout.dialog_colorize, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rcvColor)
        val removeColor = dialogView.findViewById<Button>(R.id.removeColorBtn)
        var isRemove = false
        recyclerView.layoutManager = GridLayoutManager(context, 5)
        colorAdapter = ListColorAdapter(colors, this)
        recyclerView.adapter = colorAdapter
        removeColor.setOnClickListener {
            isRemove = true
        }
        val builder = AlertDialog.Builder(requireContext()).setView(dialogView).setNegativeButton("CANCEL"){dialog, _ ->
            dialog.dismiss()
        }.setPositiveButton("OK"){dialog, which ->
            if(isRemove) selectedColor = null
            handleOkButtonClick()
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun handleOkButtonClick() {
        val selectedNotes = noteAdapter.getSelectedItems()
        selectedColor.let { color ->
            if(selectedColor.isNullOrEmpty()){
                selectedNotes.forEach { note ->
                    note.color = color
                    Log.d("TAG", "handleOkButtonClick: ${note.color}")
                    noteViewModel.updateNote(note)
                }
            }else{
                selectedNotes.forEach { note ->
                    note.color = color
                    Log.d("TAG", "handleOkButtonClick: ${note.color}")
                    noteViewModel.updateNote(note)
                }
            }
        }
        noteAdapter.notifyDataSetChanged()
        isAlternateMenuVisible = !isAlternateMenuVisible
        changeDrawerNavigationIcon()
        requireActivity().invalidateOptionsMenu()
        updateSelectedCount()
    }

    override fun onColorClick(color: String) {
        selectedColor = color
    }

    override fun onColorClick(color: String, imageView: ImageView) {

    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(
            if(isAlternateMenuVisible){
                R.menu.menu_selection
            }else{
                R.menu.top_app_bar
            }, menu
        )
        if(!isAlternateMenuVisible){
            val menuSearch = menu.findItem(R.id.search).actionView as androidx.appcompat.widget.SearchView
            menuSearch.isSubmitButtonEnabled = false
            menuSearch.setOnQueryTextListener(this)
            val sort = SpannableString(menu.findItem(R.id.sort).title)
            sort.setSpan(ForegroundColorSpan(Color.WHITE), 0, sort.length, 0)
            menu.findItem(R.id.sort).title = sort
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId){
            R.id.sort -> {
                showOptionDialog()
                true
            }
            R.id.selectAll -> {
                noteAdapter.selectAllItems()
                isAlternateMenuVisible = true
                changeBackNavigationIcon()
                requireActivity().invalidateOptionsMenu()
                updateSelectedCount()
                true
            }
            R.id.select_all_notes -> {
                noteAdapter.selectAllItems()
                isAlternateMenuVisible = true
                changeBackNavigationIcon()
                requireActivity().invalidateOptionsMenu()
                updateSelectedCount()
                true
            }
            R.id.delete -> {
                showDeleteDialog()
                true
            }
            R.id.categorize -> {
                showCategorizeDialog()
                true
            }
            R.id.export_notes_to_text_file -> {
                selectDirectory()
                true
            }
            R.id.import_text_file -> {
                openTextFile()
                true
            }
            R.id.Colorize -> {
                showColorPickerDialog()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if(newText.isNullOrEmpty()){
            noteAdapter.updateNotes(currentList)
        }else{
            searchNote(newText)
        }
        return true
    }
}