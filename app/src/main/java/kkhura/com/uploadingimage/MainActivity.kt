package kkhura.com.uploadingimage

import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_image.view.*
import java.io.ByteArrayOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var storage: FirebaseStorage
    private lateinit var databaseReference: DatabaseReference
    private lateinit var mAdapter: FirebaseRecyclerAdapter<UploadInfo, ImgViewHolder>

    private lateinit var choosePhoto: ChoosePhoto
    private var fileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        FirebaseApp.initializeApp(this)

        storage = FirebaseStorage.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference()

        fab.setOnClickListener { view ->
            choosePhoto = ChoosePhoto(this)
        }

        var query: Query = databaseReference.orderByKey();
        var layoutManager: LinearLayoutManager = LinearLayoutManager(this);
        rvList.setHasFixedSize(false);
        rvList.setLayoutManager(layoutManager);

        mAdapter = object : FirebaseRecyclerAdapter<UploadInfo, ImgViewHolder>(
                UploadInfo::class.java, R.layout.item_image, ImgViewHolder::class.java, query) {
            override fun populateViewHolder(viewHolder: ImgViewHolder?, model: UploadInfo?, position: Int) {
                viewHolder!!.tvImgName.setText(model!!.name)
                Picasso.with(baseContext)
                        .load(model.url)
                        .error(R.drawable.common_google_signin_btn_icon_dark)
                        .into(viewHolder.imgView)
            }
        }

        rvList.setAdapter(mAdapter);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                ChoosePhoto.CHOOSE_PHOTO_INTENT -> if (data != null && data.getData() != null) {
                    choosePhoto.handleGalleryResult(data)
                } else {
                    choosePhoto.handleCameraResult(choosePhoto.getCameraUri())
                }
                ChoosePhoto.SELECTED_IMG_CROP -> {
                    var imageUri = choosePhoto.getCropImageUrl()
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                    uploadFile(bitmap)
                }
            }
        }
    }


    private fun uploadFile(bmp: Bitmap) {
        var bitmap = ThumbnailUtils.extractThumbnail(bmp, 120, 120)
        var bios: ByteArrayOutputStream = ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bios)
        var byteArray = bios.toByteArray()
        var firememRef: StorageReference = storage.getReferenceFromUrl("gs://uploadfile-18622.appspot.com")
        var filePath = "" + UUID.randomUUID() + ".jpg"
        val childRef = firememRef.child(filePath)
        var uploadTask: UploadTask = childRef.putBytes(byteArray)

        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation childRef.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Log.d("Uploaded", "Success")
                writeNewImageInfoToDB(this!!.fileName!!, downloadUri.toString())
            } else {
                Log.d("Uploaded", "Unsuccessfull")
            }
        }


        uploadTask.addOnSuccessListener(this, { taskSnapshot: UploadTask.TaskSnapshot ->
            fileName = taskSnapshot.getMetadata()!!.getName()!!

        })
        uploadTask.addOnFailureListener(OnFailureListener() { exception ->
            Log.d("Uploaded", exception.message)
        })
    }

    private fun writeNewImageInfoToDB(name: String, url: String) {
        val info = UploadInfo(name, url)

        val key = databaseReference.push().getKey()
        databaseReference.child(key!!).setValue(info)
    }
}

class ImgViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imgView = view.img_view
    var tvImgName = view.tv_img_name

}
