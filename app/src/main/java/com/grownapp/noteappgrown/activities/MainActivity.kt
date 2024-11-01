package com.grownapp.noteappgrown.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.database.NoteDatabase
import com.grownapp.noteappgrown.databinding.ActivityMainBinding
import com.grownapp.noteappgrown.fragments.EditCategoriesFragment
import com.grownapp.noteappgrown.fragments.NoteWithCategoryFragment
import com.grownapp.noteappgrown.fragments.NotesFragment
import com.grownapp.noteappgrown.fragments.TrashFragment
import com.grownapp.noteappgrown.fragments.UncategorizedFragment
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.repository.CategoryRepository
import com.grownapp.noteappgrown.repository.NoteCategoryRepository
import com.grownapp.noteappgrown.repository.NoteRepository
import com.grownapp.noteappgrown.viewmodel.CategoryViewModel
import com.grownapp.noteappgrown.viewmodel.CategoryViewModelFactory
import com.grownapp.noteappgrown.viewmodel.NoteCategoryViewModel
import com.grownapp.noteappgrown.viewmodel.NoteCategoryViewModelFactory
import com.grownapp.noteappgrown.viewmodel.NoteViewModel
import com.grownapp.noteappgrown.viewmodel.NoteViewModelFactory

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    lateinit var noteViewModel: NoteViewModel
    lateinit var categoryViewModel: CategoryViewModel
    lateinit var noteCategoryViewModel: NoteCategoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setUpViewModel()
        updateCategoryList()
        setSupportActionBar(binding.topAppBar)
        var toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.topAppBar, R.string.open_nav, R.string.close_nav)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        if(savedInstanceState == null){
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, NotesFragment()).commit()
            binding.navView.setCheckedItem(R.id.nav_note)
        }

    }

    private fun updateCategoryList() {
        categoryViewModel.getAllCategory().observe(this){
            categories -> addCategoriesToDrawer(categories)
        }
    }

    private fun addCategoriesToDrawer(categories: List<Category>){
        val menuCategory = binding.navView.menu.findItem(R.id.categories)?.subMenu ?: return
        menuCategory.clear()
        for(category in categories){
            val menuItem = menuCategory.add(Menu.NONE, category.id, Menu.NONE, category.categoryName)
            menuItem.setIcon(R.drawable.ic_categorized_24)
        }
        if(categories.isNotEmpty()){
            val menuItem = menuCategory.add(Menu.NONE, R.id.fragment_uncategorized, Menu.NONE, getString(R.string.uncategorized))
            menuItem.setIcon(R.drawable.ic_uncategorized_24)
        }
        menuCategory.add(Menu.NONE, R.id.fragment_edit_categories, Menu.NONE,
            getString(R.string.str_edit_categories)).setIcon(R.drawable.ic_edit_categories_24)
    }

    private fun setUpViewModel() {
        val noteRepository = NoteRepository(NoteDatabase(this))
        val categoryRepository = CategoryRepository(NoteDatabase(this))
        val noteCategoryRepository = NoteCategoryRepository(NoteDatabase(this))
        val categoryViewModelProviderFactory = CategoryViewModelFactory(application, categoryRepository)
        val viewModelProviderFactory = NoteViewModelFactory(application, noteRepository)
        val noteCategoryViewModelFactory = NoteCategoryViewModelFactory(application,noteCategoryRepository)
        categoryViewModel = ViewModelProvider(this, categoryViewModelProviderFactory)[CategoryViewModel::class.java]
        noteViewModel = ViewModelProvider(this, viewModelProviderFactory)[NoteViewModel::class.java]
        noteCategoryViewModel = ViewModelProvider(this, noteCategoryViewModelFactory)[NoteCategoryViewModel::class.java]
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val categoryId = item.itemId
        val fragment = NoteWithCategoryFragment().apply {
            arguments = Bundle().apply {
                putInt("categoryId", categoryId)
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
        when(item.itemId){
            R.id.nav_note -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, NotesFragment()).commit()
            R.id.nav_edit_categories -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container,EditCategoriesFragment()).commit()
            R.id.nav_backup -> startActivity(Intent(this, BackupActivity::class.java))
            R.id.nav_trash -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container,TrashFragment()).commit()
            R.id.nav_setting -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.nav_rate_the_app -> false
            R.id.nav_help -> startActivity(Intent(this, HelpActivity::class.java))
            R.id.nav_privacy_policy -> startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            R.id.fragment_uncategorized -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, UncategorizedFragment()).commit()
            R.id.fragment_edit_categories -> supportFragmentManager.beginTransaction().replace(R.id.fragment_container, EditCategoriesFragment()).commit()
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            super.onBackPressed()
        }
    }
}