package com.alterpat.voicerecorder

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.alterpat.voicerecorder.db.AppDatabase
import com.alterpat.voicerecorder.db.AudioRecord
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_listing.bottomSheet
import kotlinx.android.synthetic.main.activity_listing.btnClose
import kotlinx.android.synthetic.main.activity_listing.btnDelete
import kotlinx.android.synthetic.main.activity_listing.btnRename
import kotlinx.android.synthetic.main.activity_listing.btnSelectAll
import kotlinx.android.synthetic.main.activity_listing.editorBar
import kotlinx.android.synthetic.main.activity_listing.recyclerview
import kotlinx.android.synthetic.main.activity_listing.searchInput
import kotlinx.android.synthetic.main.activity_listing.toolbar
import kotlinx.android.synthetic.main.activity_listing.tvDelete
import kotlinx.android.synthetic.main.activity_listing.tvRename
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class ListingActivity : AppCompatActivity(), Adapter.OnItemClickListener {
    private lateinit var adapter: Adapter
    private lateinit var audioRecords: List<AudioRecord>
    private lateinit var db: AppDatabase
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var menu: Menu
    private var allSelected = false
    private var nbSelected = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listing)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        audioRecords = emptyList()
        adapter = Adapter(audioRecords, this)

        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(this)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        )
            //.fallbackToDestructiveMigration()
            .build()

        fetchAll()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var query = p0.toString()
                searchDatabase("%$query%")
            }

        })

        btnSelectAll.setOnClickListener {
            allSelected = !allSelected
            Log.d("ListingTag", allSelected.toString())
            audioRecords.forEach {
                it.isChecked = allSelected
            }

            nbSelected = if (allSelected) audioRecords.size else 0
            updateBottomSheet()

            adapter.notifyDataSetChanged()
        }

        btnClose.setOnClickListener {
            closeEditor()
        }

        btnDelete.setOnClickListener {
            closeEditor()
            var toDelete: List<AudioRecord> = audioRecords.filter { it.isChecked }
            audioRecords = audioRecords.filter { !it.isChecked }
            GlobalScope.launch {
                db.audioRecordDAO().delete(toDelete)
                if (audioRecords.isEmpty())
                    fetchAll()
                else
                    adapter.setData(audioRecords)
            }
        }

        btnRename.setOnClickListener {
            Toast.makeText(this, "rename clicked", Toast.LENGTH_SHORT).show()
        }

    }

    private fun closeEditor() {
        allSelected = false
        adapter.setEditMode(false)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // hide back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        // show relative layout
        editorBar.visibility = View.GONE
        nbSelected = 0
    }

    private fun fetchAll() {
        GlobalScope.launch {
            audioRecords = db.audioRecordDAO().getAll()
            adapter.setData(audioRecords)
        }
    }

    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            audioRecords = db.audioRecordDAO().searchDatabase(query)
            runOnUiThread {
                adapter.setData(audioRecords)
            }
        }
    }

    private fun updateBottomSheet() {
        when (nbSelected) {
            0 -> {
                btnRename.isClickable = false
                btnRename.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_edit_disabled, theme)
                tvRename.setTextColor(resources.getColor(R.color.colorDisabled, theme))
                btnDelete.isClickable = false
                btnDelete.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_disabled2, theme)
                tvDelete.setTextColor(resources.getColor(R.color.colorDisabled, theme))

            }

            1 -> {
                btnRename.isClickable = true
                btnRename.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_edit, theme)
                tvRename.setTextColor(resources.getColor(R.color.colorText, theme))

                btnDelete.isClickable = true
                btnDelete.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, theme)
                tvDelete.setTextColor(resources.getColor(R.color.colorText, theme))

            }

            else -> {
                btnRename.isClickable = false
                btnRename.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_edit_disabled, theme)
                tvRename.setTextColor(resources.getColor(R.color.colorDisabled, theme))

                btnDelete.isClickable = true
                btnDelete.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, theme)
                tvDelete.setTextColor(resources.getColor(R.color.colorText, theme))

            }
        }
    }

    override fun onItemClick(position: Int) {
        var intent = Intent(this, PlayerActivity::class.java)
        var audioRecord = audioRecords[position]

        if (adapter.isEditMode()) {
            Log.d("ITEMCHANGE", audioRecord.isChecked.toString())
            audioRecord.isChecked = !audioRecord.isChecked
            adapter.notifyItemChanged(position)

            nbSelected = if (audioRecord.isChecked) nbSelected + 1 else nbSelected - 1
            updateBottomSheet()

        } else {
            intent.putExtra("filepath", audioRecord.filePath)
            intent.putExtra("filename", audioRecord.filename)
            startActivity(intent)
        }

    }

    override fun onItemLongClick(position: Int) {
        adapter.setEditMode(true)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        var audioRecord = audioRecords[position]

        audioRecord.isChecked = !audioRecord.isChecked

        nbSelected = if (audioRecord.isChecked) nbSelected + 1 else nbSelected - 1
        updateBottomSheet()

        // hide back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        // show relative layout
        editorBar.visibility = View.VISIBLE


    }

}