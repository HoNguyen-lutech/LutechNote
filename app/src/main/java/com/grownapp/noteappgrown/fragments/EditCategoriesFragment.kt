package com.grownapp.noteappgrown.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.grownapp.noteappgrown.R
import com.grownapp.noteappgrown.activities.MainActivity
import com.grownapp.noteappgrown.adapter.ListCategoryAdapter
import com.grownapp.noteappgrown.databinding.FragmentEditCategoriesBinding
import com.grownapp.noteappgrown.models.Category
import com.grownapp.noteappgrown.viewmodel.CategoryViewModel

class EditCategoriesFragment : Fragment() {
    private val binding: FragmentEditCategoriesBinding by lazy {
        FragmentEditCategoriesBinding.inflate(layoutInflater)
    }
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var categoryAdapter: ListCategoryAdapter
    private lateinit var editView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoryViewModel = (activity as MainActivity).categoryViewModel
        editView = view
        setupCategoryRecyclerView()
        binding.addBtn.setOnClickListener {
            addCategory()
        }
        (activity as MainActivity).let { mainActivity ->
            val toolbar = mainActivity.findViewById<MaterialToolbar>(R.id.topAppBar)
            toolbar.setTitle(requireContext().getString(R.string.categories))
        }
    }

    private fun setupCategoryRecyclerView() {
        categoryAdapter = ListCategoryAdapter(requireContext())
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.categoryRecyclerView.adapter = categoryAdapter
        activity?.let {
            categoryViewModel.getAllCategory().observe(viewLifecycleOwner){category ->
                categoryAdapter.differ.submitList(category)
                updateUI(category)
            }
        }
    }

    private fun updateUI(category: List<Category>) {
        if(category.isNotEmpty()){
            binding.categoryRecyclerView.visibility = View.VISIBLE
        }else{
            binding.categoryRecyclerView.visibility
        }
    }

    private fun addCategory() {
        val newCategory = Category(0, binding.newCategoryName.text.toString())
        categoryViewModel.addCategory(newCategory)
        binding.newCategoryName.text = null
        Toast.makeText(context, "Add successful", Toast.LENGTH_SHORT).show()
    }

}