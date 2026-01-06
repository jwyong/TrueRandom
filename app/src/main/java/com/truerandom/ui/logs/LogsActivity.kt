package com.truerandom.ui.logs

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.truerandom.databinding.ActivityLogsBinding
import com.truerandom.util.LogUtil

class LogsActivity: AppCompatActivity() {
    private lateinit var binding: ActivityLogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        bindUi()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)

        // Enable the back arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun bindUi() {
        LogUtil.logs.observe(this) { logs ->
            binding.tvLogs.text = logs
            binding.svLogs.post {
                binding.svLogs.fullScroll(View.FOCUS_DOWN)
            }
        }
        binding.fab.setOnClickListener {
            LogUtil.clearLogs()
        }
    }
}