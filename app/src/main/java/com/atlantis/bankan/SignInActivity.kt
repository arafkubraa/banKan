package com.atlantis.bankan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val usernameOrEmailEditText: EditText = findViewById(R.id.usernameEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val signInButton: Button = findViewById(R.id.signInButton)

        signInButton.setOnClickListener {
            val usernameOrEmail = usernameOrEmailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (usernameOrEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (usernameOrEmail.contains("@")) {
                // Kullanıcı e-posta ile giriş yapıyor
                signInWithEmail(usernameOrEmail, password)
            } else {
                // Kullanıcı kullanıcı adı ile giriş yapıyor
                signInWithUsername(usernameOrEmail, password)
            }
        }

        val signUpText: TextView = findViewById(R.id.signUpText)
        signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Giriş başarılı: ${auth.currentUser?.email}")
                    navigateToMainActivity()
                } else {
                    val errorMessage = task.exception?.message ?: "Bilinmeyen hata"
                    Log.e("FirebaseAuth", "Giriş hatası: $errorMessage")
                    Toast.makeText(this, "Giriş hatası: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithUsername(username: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val email = documents.documents[0].getString("email")
                    Log.d("Firestore", "Bulunan email: $email")

                    if (email != null) {
                        signInWithEmail(email, password)
                    } else {
                        showToast("Bu kullanıcı adı ile bağlantılı bir e-posta bulunamadı.")
                    }
                } else {
                    showToast("Kullanıcı bulunamadı.")
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Firestore hatası: ${it.message}")
                showToast("Bir hata oluştu: ${it.message}")
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@SignInActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
