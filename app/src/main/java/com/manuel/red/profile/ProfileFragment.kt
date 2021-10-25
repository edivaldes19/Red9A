package com.manuel.red.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.manuel.red.R
import com.manuel.red.databinding.FragmentProfileBinding
import com.manuel.red.package_service.MainAux
import com.manuel.red.utils.Constants
import com.manuel.red.utils.TimestampToText
import java.io.ByteArrayOutputStream
import java.util.*

class ProfileFragment : Fragment() {
    private var binding: FragmentProfileBinding? = null
    private var photoSelectedUri: Uri? = null
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding!!.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                photoSelectedUri = activityResult.data?.data
                binding?.let { view ->
                    Glide.with(this).load(photoSelectedUri).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_cloud_download)
                        .error(R.drawable.ic_error_outline).centerCrop().circleCrop()
                        .into(view.imgProfile)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding?.let { view ->
            return view.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getUser()
        configButtons()
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun getUser() {
        binding?.let { binding ->
            FirebaseAuth.getInstance().currentUser?.let { user ->
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection(Constants.COLL_USERS).document(user.uid)
                docRef.get().addOnSuccessListener { document ->
                    if (document != null) {
                        binding.tvLastModification.text =
                            "${getString(R.string.last_modification)}: ${
                                TimestampToText.getTimeAgo(document.getLong(Constants.PROP_LAST_MODIFICATION)!!)
                                    .lowercase(Locale.getDefault())
                            }."
                    }
                }.addOnFailureListener {
                    errorSnack.apply {
                        setText(getString(R.string.failed_to_get_the_last_modification))
                        show()
                    }
                }
                binding.tvSubscriberNumber.text =
                    "${getString(R.string.subscriber_number)}: ${user.uid}"
                binding.etFullName.setText(user.displayName)
                Glide.with(this).load(user.photoUrl).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_cloud_download).error(R.drawable.ic_error_outline)
                    .centerCrop().circleCrop().into(binding.imgProfile)
            }
        }
    }

    private fun configButtons() {
        binding?.let { binding ->
            binding.imgProfile.setOnClickListener {
                openGallery()
            }
            binding.btnUpdate.setOnClickListener {
                binding.etFullName.clearFocus()
                FirebaseAuth.getInstance().currentUser?.let { user ->
                    if (photoSelectedUri == null) {
                        updateUserProfile(binding, user, Uri.parse(""))
                    } else {
                        uploadReducedImage(user)
                    }
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private fun updateUserProfile(binding: FragmentProfileBinding, user: FirebaseUser, uri: Uri) {
        if (binding.etFullName.text.isNullOrEmpty()) {
            binding.tilFullName.run {
                error = getString(R.string.this_field_is_required)
                requestFocus()
            }
        } else {
            binding.tilFullName.error = null
            val profileUpdated = UserProfileChangeRequest.Builder()
                .setDisplayName(binding.etFullName.text.toString().trim()).setPhotoUri(uri).build()
            user.updateProfile(profileUpdated).addOnSuccessListener {
                val userMap = hashMapOf<String, Any>(
                    Constants.PROP_LAST_MODIFICATION to Date().time,
                    Constants.PROP_USERNAME to user.displayName.toString(),
                    Constants.PROP_PROFILE_PICTURE to user.photoUrl.toString()
                )
                val db = FirebaseFirestore.getInstance()
                db.collection(Constants.COLL_USERS).document(user.uid).update(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(
                            activity,
                            getString(R.string.edited_profile_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                        (activity as? MainAux)?.updateTitle(user)
                        activity?.onBackPressed()
                    }.addOnFailureListener {
                        errorSnack.apply {
                            setText(getString(R.string.error_editing_profile_with_firebase_firestore))
                            show()
                        }
                    }
            }.addOnFailureListener {
                errorSnack.apply {
                    setText(getString(R.string.error_editing_profile_with_firebase_ui))
                    show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun uploadReducedImage(user: FirebaseUser) {
        val profileRef =
            FirebaseStorage.getInstance().reference.child(user.uid).child(Constants.PATH_PROFIlE)
                .child(Constants.MY_IMAGE)
        photoSelectedUri?.let { uri ->
            binding?.let { binding ->
                getBitmapFromUri(uri)?.let { bitmap ->
                    binding.progressBar.visibility = View.VISIBLE
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                    profileRef.putBytes(byteArrayOutputStream.toByteArray())
                        .addOnProgressListener { taskSnapshot ->
                            val progress =
                                (100 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                            taskSnapshot.run {
                                binding.progressBar.progress = progress
                                binding.tvProgress.text =
                                    "${getString(R.string.uploading_image)} ${
                                        String.format(
                                            "%s%%",
                                            progress
                                        )
                                    }"
                            }
                        }.addOnCompleteListener {
                            binding.progressBar.visibility = View.INVISIBLE
                            binding.tvProgress.text = ""
                        }.addOnSuccessListener { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                                updateUserProfile(binding, user, downloadUrl)
                            }
                        }.addOnFailureListener {
                            errorSnack.apply {
                                setText(getString(R.string.image_upload_error))
                                show()
                            }
                        }
                }
            }
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        activity?.let { fragmentActivity ->
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(fragmentActivity.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(fragmentActivity.contentResolver, uri)
            }
            return getResizedImage(bitmap)
        }
        return null
    }

    private fun getResizedImage(image: Bitmap): Bitmap {
        var width = image.width
        var height = image.height
        if (width <= 256 && height <= 256) return image
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = 256
            height = (width / bitmapRatio).toInt()
        } else {
            height = 256
            width = (height / bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }
}