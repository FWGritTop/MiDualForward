package com.notifyforwarder.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.notifyforwarder.R
import com.notifyforwarder.databinding.ActivityAppManagerBinding
import com.notifyforwarder.manager.AppManager
import com.notifyforwarder.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppManagerActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    
    private lateinit var binding: ActivityAppManagerBinding
    private val allApps = mutableListOf<AppInfo>()
    private val filteredApps = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter
    private lateinit var searchEdit: EditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        searchEdit = findViewById(R.id.search_edit)
        
        adapter = AppAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        loadApps()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        AppManager.reload(this)
    }
    
    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        
        launch {
            withContext(Dispatchers.IO) {
                AppManager.init(this@AppManagerActivity)
                allApps.clear()
                allApps.addAll(AppManager.getAppList())
            }
            
            binding.progressBar.visibility = View.GONE
            filterApps("")
        }
    }
    
    private fun filterApps(query: String) {
        filteredApps.clear()
        
        if (query.isBlank()) {
            val enabled = allApps.filter { it.isForwardEnabled }
            val disabled = allApps.filter { !it.isForwardEnabled }
            filteredApps.addAll(enabled)
            filteredApps.addAll(disabled)
        } else {
            val lowerQuery = query.lowercase()
            val matched = allApps.filter { 
                it.displayName.lowercase().contains(lowerQuery) || 
                it.packageName.lowercase().contains(lowerQuery)
            }
            val enabled = matched.filter { it.isForwardEnabled }
            val disabled = matched.filter { !it.isForwardEnabled }
            filteredApps.addAll(enabled)
            filteredApps.addAll(disabled)
        }
        
        adapter.notifyDataSetChanged()
        
        binding.emptyView.visibility = if (filteredApps.isEmpty()) View.VISIBLE else View.GONE
    }
    
    inner class AppAdapter : RecyclerView.Adapter<AppAdapter.Holder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return Holder(view)
        }
        
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val app = filteredApps[position]
            
            holder.name.text = app.displayName
            holder.pkg.text = app.packageName
            
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(app.packageName, 0)
                holder.icon.setImageDrawable(pm.getApplicationIcon(appInfo))
            } catch (e: Exception) {
                holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            holder.switch.setOnCheckedChangeListener(null)
            holder.switch.isChecked = app.isForwardEnabled
            
            holder.switch.setOnCheckedChangeListener { _, isChecked ->
                AppManager.setEnabled(this@AppManagerActivity, app.appKey, isChecked)
                app.isForwardEnabled = isChecked
                
                val index = allApps.indexOfFirst { it.appKey == app.appKey }
                if (index >= 0) {
                    allApps[index].isForwardEnabled = isChecked
                }
                
                filterApps(searchEdit.text.toString())
            }
            
            holder.itemView.setOnClickListener {
                holder.switch.toggle()
            }
            
            holder.dualTag.visibility = if (app.isDualApp) View.VISIBLE else View.GONE
        }
        
        override fun getItemCount() = filteredApps.size
        
        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.ivAppIcon)
            val name: TextView = view.findViewById(R.id.tvAppName)
            val pkg: TextView = view.findViewById(R.id.tvPackageName)
            val switch: SwitchCompat = view.findViewById(R.id.switchEnable)
            val dualTag: TextView = view.findViewById(R.id.tvDualTag)
        }
    }
}
