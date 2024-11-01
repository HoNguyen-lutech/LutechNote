package com.grownapp.noteappgrown.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.activities.MainActivity
import com.grownapp.noteappgrown.adapter.ListCategoryAdapter
import com.grownapp.noteappgrown.adapter.ListNoteAdapter
import com.grownapp.noteappgrown.databinding.FragmentTrashBinding
import com.grownapp.noteappgrown.listeners.OnItemClickListener
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.models.Note
import com.grownapp.noteappgrown.viewmodel.CategoryViewModel
import com.grownapp.noteappgrown.viewmodel.NoteCategoryViewModel
import com.grownapp.noteappgrown.viewmodel.NoteViewModel
import java.io.IOException

class TrashFragment : Fragment(R.layout.fragment_trash), MenuProvider {
    private val binding: FragmentTrashBinding by lazy {
        FragmentTrashBinding.inflate(layoutInflater)
    }
    private lateinit var noteViewModel: NoteViewModel
    private lateinit var  noteCategoryViewModel: NoteCategoryViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var noteAdapter: ListNoteAdapter
    private lateinit var categoryAdapter: ListCategoryAdapter
    private lateinit var categories: List<Category>
    private lateinit var currentList: List<Note>
    companion object{
        private const val REQUEST_WRITE_PERMISSION = 1001
        private const val REQUEST_CODE_PICK_DIRECTORY = 1002
    }
    private var isAlternateMenuVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
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
        setUpNoteRecyclerView()
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
            toolbar.setTitle(requireContext().getString(R.string.trash))
        }
    }

    private fun setUpNoteRecyclerView() {
        noteAdapter = ListNoteAdapter(requireContext(), object : OnItemClickListener{
            override fun onNoteClick(note: Note, isChoose: Boolean) {
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
        noteAdapter.isInTrash = true
        categoryAdapter = ListCategoryAdapter(requireContext())
        binding.listTrashRcv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.listTrashRcv.adapter = noteAdapter
        activity?.let {
            noteViewModel.getAllTrashNotes().observe(viewLifecycleOwner){note ->
                noteAdapter.differ.submitList(note)
                currentList = noteAdapter.differ.currentList
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

    private fun updateUI(note: List<Note>) {
        if(note.isNotEmpty()){
            binding.listTrashRcv.visibility = View.VISIBLE
        }else{
            binding.listTrashRcv.visibility = View.GONE
        }
    }

    private fun changeDrawerNavigationIcon(){
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
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
    private fun restoreNote(){
        val selectedNotes = noteAdapter.getSelectedItems()
        val selectedIds = selectedNotes.map { it.id }
        noteViewModel.restoreFromTrash(selectedIds)
        noteAdapter.removeSelectedItems()
        isAlternateMenuVisible = false
        changeDrawerNavigationIcon()
        requireActivity().invalidateOptionsMenu()
        updateSelectedCount()
    }
    private fun showRestoreAllNoteDialog(){
        val builer = AlertDialog.Builder(context).setMessage("Restore all notes?").setNegativeButton("No"){dialog, _ ->
            dialog.dismiss()
        }.setPositiveButton("Yes"){dialog, _ ->
            val selectedItem = noteAdapter.differ.currentList
            val selectedIds = selectedItem.map { it.id }
            noteViewModel.restoreFromTrash(selectedIds)
            dialog.dismiss()
        }
        builer.create().show()
    }
    private fun showEmptyTrashDialog(){
        val builder = AlertDialog.Builder(context)
            .setMessage("All trashed notes will be deleted permanently. Are you sure that you want to delete all of the trashed notes?")
            .setNegativeButton("No"){dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton("Yes"){dialog, _ ->
                val selectedItem = noteAdapter.differ.currentList
                val selectedIds = selectedItem.map { it.id }
                noteViewModel.deleteNote(selectedIds)
                dialog.dismiss()
            }
        builder.create().show()
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
            selectedNotes = noteAdapter.differ.currentList.toSet()
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
            val docUri = DocumentsContract.createDocument(
                requireContext().contentResolver,
                documentUri,
                "text/plain",
                fileName
            )
            docUri?.let {
                requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
            }
        } catch (e: IOException){
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
    }
    private fun deleteInTrash(){
        val selectedItem = noteAdapter.getSelectedItems()
        val selectedIds = selectedItem.map { it.id }
        val builder = AlertDialog.Builder(context)
            .setMessage("Are you sure that you want to delete the selected notes? The notes will be deleted permanently")
            .setNegativeButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }.setPositiveButton("Ok"){dialog, _ ->
                noteViewModel.deleteNote(selectedIds)
                noteAdapter.clearSelection()
                noteAdapter.removeSelectedItems()
                isAlternateMenuVisible = false
                changeDrawerNavigationIcon()
                requireActivity().invalidateOptionsMenu()
                updateSelectedCount()
                dialog.dismiss()
            }
        builder.create().show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if(isAlternateMenuVisible){
            menu.clear()
            menuInflater.inflate(R.menu.menu_trash_selection, menu)
        }else{
            menu.clear()
            menuInflater.inflate(R.menu.menu_trash, menu)
            (activity as MainActivity).let { mainActivity ->
                val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
                toolbar.setTitle(requireContext().getString(R.string.trash))
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId){
            R.id.selectAll -> {
                noteAdapter.selectAllItem()
                isAlternateMenuVisible = true
                changeBackNavigationIcon()
                requireActivity().invalidateOptionsMenu()
                updateSelectedCount()
                true
            }
            R.id.delete -> {
                deleteInTrash()
                true
            }
            R.id.restore -> {
                restoreNote()
                true
            }
            R.id.undeleteAll -> {
                showRestoreAllNoteDialog()
                true
            }
            R.id.export_notes_to_text_file -> {
                selectDirectory()
                true
            }
            R.id.emptyTrash -> {
                showEmptyTrashDialog()
                true
            }else -> {
                false
            }
        }
    }
}