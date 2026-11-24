package org.setu.placemark

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.setu.placemark.models.PlacemarkModel
import android.net.Uri
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import kotlin.jvm.java


class AddEditActivity : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var xInput: EditText
    private lateinit var yInput: EditText

    private var editingId: Long? = null
    private lateinit var imagePreview: ImageView
    private var selectedImageUri: Uri? = null

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Provider does not support persistable access.
                }

                selectedImageUri = uri
                imagePreview.setImageURI(uri)
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_edit)

        imagePreview = findViewById(R.id.markImage)

        findViewById<Button>(R.id.chooseImageButton)
            .setOnClickListener {
                imagePicker.launch(arrayOf("image/*"))
            }

        titleInput =
            findViewById(R.id.titleInput)

        descriptionInput =
            findViewById(R.id.descriptionInput)

        xInput =
            findViewById(R.id.xInput)

        yInput =
            findViewById(R.id.yInput)

        findViewById<Button>(R.id.buttonLoc).setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)

            val latitude = xInput.text.toString().toDoubleOrNull()
            val longitude = yInput.text.toString().toDoubleOrNull()

            if (
                latitude != null && longitude != null &&
                latitude.isFinite() && longitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0
            ) {
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
            }

            intent.putExtra(
                "markerTitle",
                titleInput.text.toString().trim()
            )

            mapPicker.launch(intent)
        }

        val saveButton =
            findViewById<Button>(R.id.saveButton)

        val cancelButton =
            findViewById<Button>(R.id.cancelButton)

        editingId =
            intent.getLongExtra("id", -1L)
                .takeIf { it != -1L }

        if (editingId != null) {

            findViewById<TextView>(R.id.formTitle)
                .text = "Edit Mark"

            loadExistingMark(editingId!!)
        }

        saveButton.setOnClickListener {
            saveMark()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private val mapPicker =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                xInput.setText(
                    data.getDoubleExtra("latitude", 0.0).toString()
                )

                yInput.setText(
                    data.getDoubleExtra("longitude", 0.0).toString()
                )
            }
        }

    @SuppressLint("UseKtx")
    private fun loadExistingMark(id: Long) {

        val mark =
            AppData.placedMarks.findOne(id)

        if (mark == null) {

            Toast.makeText(
                this,
                "Mark not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        titleInput.setText(mark.title)
        descriptionInput.setText(mark.desc)
        xInput.setText(mark.x.toString())
        yInput.setText(mark.y.toString())
        selectedImageUri = mark.imageUri?.let { Uri.parse(it) }

        if (selectedImageUri != null) {
            imagePreview.setImageURI(selectedImageUri)
        } else {
            imagePreview.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun saveMark() {
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

        if (title.isEmpty()) {
            titleInput.error = "Title is required"
            return
        }

        val x = xInput.text.toString().toDoubleOrNull()

        if (x == null) {
            xInput.error = "Enter a valid number"
            return
        }

        val y = yInput.text.toString().toDoubleOrNull()

        if (y == null) {
            yInput.error = "Enter a valid number"
            return
        }

        try {
            val mark = PlacemarkModel(
                id = editingId ?: 0L,
                title = title,
                desc = description,
                x = x,
                y = y,
                imageUri = selectedImageUri?.toString()
            )

            // The store calls validate() before saving.
            if (editingId == null) {
                AppData.placedMarks.create(mark)

                Toast.makeText(
                    this,
                    "Mark created",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val updated = AppData.placedMarks.update(mark)

                if (!updated) {
                    Toast.makeText(
                        this,
                        "Mark not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                Toast.makeText(
                    this,
                    "Mark updated",
                    Toast.LENGTH_SHORT
                ).show()
            }

            finish()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(
                this,
                e.message ?: "Please check the entered values.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
