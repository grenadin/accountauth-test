package org.microg.test.accountauth

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AccountAuthTest"
private const val GOOGLE_ACCOUNT_TYPE = "com.google"
private const val DEFAULT_SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.email"
private const val REQUEST_CHOOSE_ACCOUNT = 1

class MainActivity : Activity() {
    private lateinit var scopeInput: EditText
    private lateinit var logView: TextView
    private var account: Account? = null
    private var overridePackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (16 * resources.displayMetrics.density).toInt()
        scopeInput = EditText(this).apply { setText(DEFAULT_SCOPE) }
        logView = TextView(this).apply { setTextIsSelectable(true) }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(scopeInput)
            addView(Button(context).apply {
                text = "Choose account"
                setOnClickListener { chooseAccount() }
            })
            addView(Button(context).apply {
                text = "Request token (AccountManager.getAuthToken)"
                setOnClickListener { requestToken() }
            })
            addView(logView)
        }
        setContentView(ScrollView(this).apply { addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) })
        log("Package: $packageName")
        val prefs = getSharedPreferences("account", MODE_PRIVATE)
        val savedName = prefs.getString("name", null)
        val savedType = prefs.getString("type", null)
        if (savedName != null && savedType != null) account = Account(savedName, savedType)
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("account")?.let { account = Account(it, GOOGLE_ACCOUNT_TYPE) }
        overridePackage = intent?.getStringExtra("override")
        val scope = intent?.getStringExtra("scope") ?: return
        scopeInput.setText(scope)
        requestToken()
    }

    private fun chooseAccount() {
        val intent = AccountManager.newChooseAccountIntent(account, null, arrayOf(GOOGLE_ACCOUNT_TYPE), null, null, null, null)
        startActivityForResult(intent, REQUEST_CHOOSE_ACCOUNT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CHOOSE_ACCOUNT) return
        val name = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val type = data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
        if (resultCode == RESULT_OK && name != null && type != null) {
            account = Account(name, type)
            getSharedPreferences("account", MODE_PRIVATE).edit().putString("name", name).putString("type", type).apply()
            log("Account chosen")
        } else {
            log("Account chooser cancelled (resultCode=$resultCode)")
        }
    }

    private fun requestToken() {
        val overrides = overridePackage?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        if (overrides.isNullOrEmpty()) requestToken(null) else overrides.forEach { requestToken(it) }
    }

    private fun requestToken(override: String?) {
        val account = account ?: return log("Choose an account first")
        val scope = scopeInput.text.toString().trim()
        val options = Bundle()
        override?.let { options.putString("overridePackage", it) }
        val label = "$scope, override=$override"
        log("getAuthToken: $label, waiting for result...")
        AccountManager.get(this).getAuthToken(account, scope, options, this, { future ->
            try {
                val result = future.result
                val token = result.getString(AccountManager.KEY_AUTHTOKEN)
                if (token != null) {
                    log("SUCCESS [$label]: token received (${token.length} chars)")
                } else {
                    log("Result without token [$label], keys: ${result.keySet()}")
                }
            } catch (e: Exception) {
                log("FAILED [$label]: ${e.javaClass.simpleName}: ${e.message}")
            }
        }, null)
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        logView.append("$time  $message\n")
    }
}
