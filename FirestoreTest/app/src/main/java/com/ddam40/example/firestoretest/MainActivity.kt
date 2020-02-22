package com.ddam40.example.firestoretest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {

    data class UserDTO(var name: String = "", var address: String = "")

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseStorage.getInstance().reference.child("bg.jpg")
            .downloadUrl.addOnCompleteListener { task ->
            println("다운로드 URL : ${task.result}")
        }
    }
}
