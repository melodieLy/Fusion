package fr.intechinfo.fusionandroid


import android.app.Activity
import android.content.Intent

import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.support.v7.widget.Toolbar
import android.support.v4.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import android.provider.Settings.System.DEFAULT_RINGTONE_URI
import android.media.RingtoneManager
import android.media.Ringtone
import android.provider.Settings
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.widget.Toast
import com.pubnub.api.Pubnub


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    //For design
    private var toolbar: Toolbar? = null
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    lateinit var mPubNub: Pubnub
    private var fragmentNews: Fragment? = null

    private val RC_SIGN_IN = 28

    public override fun onStart() {
        super.onStart()
        //val account = GoogleSignIn.getLastSignedInAccount(this)
    }

     fun LaunchURL(url: String?) {

        val thread = object : Thread() {
            override fun run() {
                try {
                    Looper.prepare()
                    Toast.makeText(this@MainActivity, "Url Received", Toast.LENGTH_SHORT).show()
                    Thread.sleep(5000)
                    val uris = Uri.parse(url)
                    val browserIntent = Intent(Intent.ACTION_VIEW, uris)
                    startActivity(browserIntent)

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }

        thread.start()

    }

    fun initPubNub()
    {
      /*  val stdbyChannel = "test" + Constants.STDBY_SUFFIX
        this.mPubNub = Pubnub(Constants.PUB_KEY, Constants.SUB_KEY)
        this.mPubNub.setUUID("test")
        this.mPubNub.subscribe("test", Callback(this))*/
        Rtc.instance.initRtcAudio(this)
    }

    public fun batteryStatus(){
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter -> this.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // How are we charging?
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level / scale.toFloat()
        }
    }

    fun storageStatus(){
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val toGiga = 1024.0f * 1024.0f * 1024.0f
        val totalGiga = stat.totalBytes / toGiga
        val freeGiga = stat.freeBytes / toGiga
        val usedGiga = totalGiga - freeGiga
        Log.d("storageStatus", totalGiga.toString())
        Log.d("storageStatus", freeGiga.toString())
        Log.d("storageStatus", usedGiga.toString())
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult<ApiException>(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            // updateUI(account);
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            //updateUI(null);
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        storageStatus()

        //-------------------------
        val retrofitAPI = HttpExecute.BuildAPI()
        val cc = ContentCollector()
        val contactLs = ContactsList()
        contactLs.SetContact(cc.GetContacts(this))
        Log.d("SYNC", "Name : " + contactLs.GetContact()!![1].GetName() + " Number : " + contactLs.GetContact()!![1].GetNumber())
        val SMSLs = SMSList()
        SMSLs.SetSMS(cc.GetSMS(this))
        Log.d("SYNC", "Address : " + SMSLs.GetSMS()!![3].GetAddress() + " Message : " + SMSLs.GetSMS()!![3].GetBody() + " Date : " + SMSLs.GetSMS()!![3].GetDate() + " Type : " + SMSLs.GetSMS()!![3].GetType())

        HttpExecute(retrofitAPI.CreateContacts(contactLs)).start()
        HttpExecute(retrofitAPI.CreateSMS(SMSLs)).start()
        //-----------------------------------------
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        batteryStatus()
        //Configure all views
        this.configureToolBar()
        this.configureDrawerLayout()
        this.configureNavigationView()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

        //onNewIntent(intent)
        FirebaseMessaging.getInstance().subscribeToTopic("googleId")
        PermissionUtil.initPermissions(this)
        //val retrofitAPI = HttpExecute.BuildAPI()
        val token = Token()
        token.SetToken(FirebaseInstanceId.getInstance().token!!)
        val stringCall = retrofitAPI.CreateNewDevice(token)
        val t = HttpExecute(stringCall)
        t.start()
        //syncDataTest();
        initPubNub()
    }

    protected fun syncDataTest() {
        val retrofitAPI = HttpExecute.BuildAPI()
        val cc = ContentCollector()
        val contactLs = ContactsList()
        contactLs.SetContact(cc.GetContacts(this))
        Log.d("SYNC", "Name : " + contactLs.GetContact()?.get(1)!!.GetName() + " Number : " + contactLs.GetContact()!![1].GetNumber())
        val SMSLs = SMSList()
        SMSLs.SetSMS(cc.GetSMS(this))
        Log.d("SYNC", "Address : " + SMSLs.GetSMS()?.get(3)!!.GetAddress() + " Message : " + SMSLs.GetSMS()!![3].GetBody() + " Date : " + SMSLs.GetSMS()!![3].GetDate() + " Type : " + SMSLs.GetSMS()!![3].GetType())

        HttpExecute(retrofitAPI.CreateContacts(contactLs)).start()
        HttpExecute(retrofitAPI.CreateSMS(SMSLs)).start()
    }

    override fun onBackPressed() {
        // Handle back click to close menu
        if (this.drawerLayout!!.isDrawerOpen(GravityCompat.START)) {
            this.drawerLayout!!.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        // Handle Navigation Item Click
        val id = item.itemId

        when (id) {
            R.id.activity_main_drawer_news -> this.showFragment(FRAGMENT_NEWS)
            R.id.activity_main_drawer_profile -> {
            }
            R.id.activity_main_drawer_settings -> {
            }
            else -> {
            }
        }

        this.drawerLayout!!.closeDrawer(GravityCompat.START)

        return true
    }

    private fun configureToolBar() {
        this.toolbar = findViewById(R.id.activity_main_toolbar)
        setSupportActionBar(toolbar)
    }

    private fun configureDrawerLayout() {
        this.drawerLayout = findViewById(R.id.activity_main_drawer_layout)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout!!.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun configureNavigationView() {
        this.navigationView = findViewById(R.id.activity_main_nav_view)
        navigationView!!.setNavigationItemSelectedListener(this)
    }

    private fun showFragment(fragmentIdentifier: Int) {
        when (fragmentIdentifier) {
            FRAGMENT_NEWS -> this.showNewsFragment()
            else -> {
            }
        }
    }

    private fun showNewsFragment() {
        if (this.fragmentNews == null) this.fragmentNews = NewsFragment.newInstance()
        this.startTransactionFragment(this.fragmentNews!!)
    }

    private fun startTransactionFragment(fragment: Fragment) {
        if (!fragment.isVisible) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.activity_main_frame_layout, fragment).commit()
        }
    }

    companion object {
        //For data - Identify each fragment with a number
        private val FRAGMENT_NEWS = 0
        private val TAG = "MainActivity"

    }

}
