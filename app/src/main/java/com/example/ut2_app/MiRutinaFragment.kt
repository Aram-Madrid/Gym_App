package com.example.ut2_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ut2_app.databinding.FragmentHomeBinding
import com.example.ut2_app.databinding.FragmentMiRutinaBinding

class MiRutinaFragment : Fragment() {

    private var _binding: FragmentMiRutinaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMiRutinaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
