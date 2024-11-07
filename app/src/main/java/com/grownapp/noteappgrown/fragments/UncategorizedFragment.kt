package com.grownapp.noteappgrown.fragments

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
import android.widget.SearchView
import android.widget.Toast
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.activities.EditNoteActivity
import com.grownapp.noteappgrown.activities.MainActivity
import com.grownapp.noteappgrown.adapter.ListCategoryAdapter
import com.grownapp.noteappgrown.adapter.ListNoteAdapter
import com.grownapp.noteappgrown.databinding.FragmentUncategorizedBinding
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

class UncategorizedFragment : Fragment(R.layout.fragment_uncategorized), MenuProvider, androidx.appcompat.widget.SearchView.OnQueryTextListener {
    private val binding: FragmentUncategorizedBinding by lazy {
        FragmentUncategorizedBinding.inflate(layoutInflater)
    }
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var noteCategoryViewModel: NoteCategoryViewModel
    private lateinit var noteAdapter: ListNoteAdapter
    private lateinit var categoryAdapter: ListCategoryAdapter
    private lateinit var uncategorizedView: View
    private lateinit var categories: List<Category>
    private lateinit var currentList: List<Note>
    private var isAlternateMenuVisible: Boolean = false
    companion object{
        private const val READ_FILE_REQUEST_CODE = 101
        private const val REQUEST_WRITE_PERMISSION = 1001
        private const val REQUEST_CODE_PICK_DIRECTORY = 1002
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

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
        uncategorizedView = view
        setUpNoteRecyclerView()
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
            toolbar.setTitle(requireContext().getString(R.string.notepadFree))
        }
        binding.addNoteFab.setOnClickListener { addNote() }
    }

    private fun setUpNoteRecyclerView() {
        noteAdapter = ListNoteAdapter(requireContext(), object : OnItemClickListener{
            override fun onNoteClick(note: Note, isChoose: Boolean) {
                if(!isChoose && !isAlternateMenuVisible){
                    val intent = Intent(activity, EditNoteActivity::class.java)
                    intent.putExtra("id", note.id)
                    intent.putExtra("title", note.title)
                    intent.putExtra("content", note.content)
                    intent.putExtra("created", note.created)
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
        binding.uncategorizedNote.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.uncategorizedNote.adapter = noteAdapter
        activity?.let {
            noteViewModel.getNotesWithoutCategory().observe(viewLifecycleOwner){note ->
                noteAdapter.updateNotes(note)
                currentList = noteAdapter.getNotes()
                updateUI(note)
            }
        }
        activity?.let {
            categoryViewModel.getAllCategory().observe(viewLifecycleOwner){category ->
                categoryAdapter.differ.submitList(category)
                categories = categoryAdapter.differ.currentList
            }
        }
    }

    private fun updateUI(note: List<Note>) {
        if(note.isNotEmpty()){
            binding.uncategorizedNote.visibility = View.VISIBLE
        }else{
            binding.uncategorizedNote.visibility = View.GONE
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
                toolbar.navigationIcon?.setTint(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                toolbar.setNavigationOnClickListener {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                toolbar.setTitle(requireContext().getString(R.string.notepadFree))
            }

        }
    }

    private fun addNote() {
        val note = Note(0, "", "", getCurrentTime(), getCurrentTime(), null, false)
        noteViewModel.addNote(note)
        val intent = Intent(context, EditNoteActivity::class.java)
        intent.putExtra("id", note.id)
        intent.putExtra("title", note.title)
        intent.putExtra("content", note.content)
        intent.putExtra("created", note.created)
        startActivity(intent)
        Toast.makeText(context, "Add successful", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentTime(): String {
        val calendar = Calendar.getInstance()
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
        return formattedDate
    }

    private fun showCategorizeDialog(){
        val checkedItem = BooleanArray(categories.size)
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Select category")
            .setPositiveButton("Ok"){dialog, which ->
                val selectedCategories = mutableListOf<Category>()
                for (i in categories.indices){
                    if(checkedItem[i]){
                        selectedCategories.add(categories[i])
                    }
                }
                val noteCategoryCrossRefs = mutableListOf<NoteCategoryCrossRef>()
                val selectedNotes = noteAdapter.getSelectedItems()
                for(noteId in selectedNotes.map { it.id }){
                    for(categoryId in selectedCategories.map { it.id }){
                        noteCategoryCrossRefs.add(NoteCategoryCrossRef(noteId, categoryId))
                    }
                }
                noteCategoryViewModel.addListNoteCategory(noteCategoryCrossRefs)
                Toast.makeText(requireContext(), "Notes and categories linked successfully", Toast.LENGTH_SHORT).show()
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

    private fun changeDrawerNavigationIcon() {
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<Toolbar>(R.id.topAppBar)
            val drawerLayout = mainActivity.findViewById<DrawerLayout>(R.id.drawerLayout)
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
            }
        }
    }

    private fun showDeleteDialog(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Delete")
            .setMessage("Do you want to delete?")
            .setPositiveButton("Delete"){dialog, which ->
                deleteSelectedItem()
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun deleteSelectedItem() {
        val selectedNotes = noteAdapter.getSelectedItems()
        val selectedIds = selectedNotes.map { it.id }
        noteViewModel.deleteNote(selectedIds)
        noteAdapter.removeSelectedItems()
    }

    private fun showOptionDialog(){
        val sortOption = arrayOf(
            "edit date: from newest",
            "edit date: from oldest",
            "title: A to Z",
            "title: Z to A",
            "creation date: from newest",
            "creation date: from oldest"
        )
        var selectedOption = 0
        val noteList = noteAdapter.getNotes()
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Sort by")
            .setPositiveButton("Sort"){dialog, which ->
                when (selectedOption){
                    0 -> sortByEditDateNewest(noteList)
                    1 -> sortByEditDateOldest(noteList)
                    2 -> sortByTitleAToZ(noteList)
                    3 -> sortByTitleZToA(noteList)
                    4 -> sortByCreationDateNewest(noteList)
                    5 -> sortByCreationDateOldest(noteList)
                }
            }.setNegativeButton("Cancel"){dialog, which ->
                dialog.dismiss()
            }.setSingleChoiceItems(sortOption, selectedOption){dialog, which ->
                selectedOption = which
            }
        builder.create().show()
    }

    private fun sortByCreationDateOldest(noteList: List<Note>) {
        return noteAdapter.updateNotes(noteList.sortedBy { it.id })
    }

    private fun sortByCreationDateNewest(noteList: List<Note>) {
        return noteAdapter.updateNotes(noteList.sortedByDescending { it.id })
    }

    private fun sortByTitleZToA(noteList: List<Note>) {
        return noteAdapter.updateNotes(noteList.sortedByDescending { it.title })
    }

    private fun sortByTitleAToZ(noteList: List<Note>) {
        return noteAdapter.updateNotes(noteList.sortedBy { it.title })
    }

    private fun sortByEditDateOldest(noteList: List<Note>) {
        return noteAdapter.updateNotes(noteList.sortedBy { it.time })
    }

    private fun sortByEditDateNewest(noteList: List<Note>) {
        return noteAdapter.updateNotes(noteList.sortedByDescending { it.time })
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
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
            val docUri = DocumentsContract.createDocument(requireContext().contentResolver, documentUri, "text/plain", fileName)
            docUri?.let {
                requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
            }
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == REQUEST_CODE_PICK_DIRECTORY && resultCode == Activity.RESULT_OK){
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
            while(reader.readLine().also { line= it } != null){
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
                val fileName = if (displayNameIndex != -1) it.getString(displayNameIndex) else "Unknown"
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
            R.id.delete -> {
                showDeleteDialog()
                true
            }
            R.id.categorize -> {
                showCategorizeDialog()
                true
            }
            R.id.import_text_file -> {
                openTextFile()
                true
            }
            R.id.export_notes_to_text_file -> {
                selectDirectory()
                true
            }else -> {
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