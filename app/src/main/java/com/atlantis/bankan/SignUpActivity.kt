package com.atlantis.bankan

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val usernameEditText: EditText = findViewById(R.id.usernameEditText)
        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val confirmPasswordEditText: EditText = findViewById(R.id.confirmPasswordEditText)
        val signUpButton: Button = findViewById(R.id.signUpButton)
        val signInButton: Button = findViewById(R.id.signInButton)

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (!validateInputs(username, email, password, confirmPassword)) return@setOnClickListener

            checkUserExists(username, email) { exists ->
                if (!exists) {
                    registerUser(username, email, password)
                } else {
                    showToast("Bu kullanıcı adı veya e-posta zaten kayıtlı!")
                }
            }
        }

        signInButton.setOnClickListener {
            navigateToSignIn()
        }
    }

    private fun validateInputs(username: String, email: String, password: String, confirmPassword: String): Boolean {
        when {
            username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                showToast("Lütfen tüm alanları doldurun!")
                return false
            }
            password.length < 6 -> {
                showToast("Şifre en az 6 karakter olmalıdır!")
                return false
            }
            password != confirmPassword -> {
                showToast("Şifreler eşleşmiyor!")
                return false
            }
        }
        return true
    }

    private fun checkUserExists(username: String, email: String, onComplete: (Boolean) -> Unit) {
        val usernameQuery = firestore.collection("users").whereEqualTo("username", username).get()
        val emailQuery = firestore.collection("users").whereEqualTo("email", email).get()

        usernameQuery.addOnSuccessListener { usernameResult ->
            emailQuery.addOnSuccessListener { emailResult ->
                val exists = !usernameResult.isEmpty || !emailResult.isEmpty
                if (exists) {
                    Log.d("SignUpActivity", "Kullanıcı adı veya e-posta zaten mevcut.")
                }
                onComplete(exists)
            }.addOnFailureListener { e ->
                Log.e("SignUpActivity", "E-posta kontrol hatası: ${e.message}")
                showToast("Bir hata oluştu, lütfen tekrar deneyin.")
                onComplete(false)
            }
        }.addOnFailureListener { e ->
            Log.e("SignUpActivity", "Kullanıcı adı kontrol hatası: ${e.message}")
            showToast("Bir hata oluştu, lütfen tekrar deneyin.")
            onComplete(false)
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userData = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "uid" to user?.uid
                    )

                    firestore.collection("users")
                        .document(user!!.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            showToast("Kayıt başarılı! Giriş ekranına yönlendiriliyorsunuz.")
                            navigateToSignIn()
                        }
                        .addOnFailureListener { e ->
                            Log.e("SignUpActivity", "Firestore kayıt hatası: ${e.message}")
                            showToast("Kullanıcı verileri kaydedilirken hata oluştu!")
                        }
                } else {
                    Log.e("SignUpActivity", "Auth kayıt hatası: ${task.exception?.message}")
                    showToast("Kayıt hatası: ${task.exception?.localizedMessage}")
                }
            }
    }

    private fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
