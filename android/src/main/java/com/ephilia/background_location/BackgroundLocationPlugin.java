package com.ephilia.background_location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** BackgroundLocationPlugin */
public class BackgroundLocationPlugin extends FlutterActivity implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  public LiveTrackingService gpsService;
  public boolean mTracking = false;
  private Context mContext;


  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    this.mContext = flutterPluginBinding.getApplicationContext();
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "background_location");
    channel.setMethodCallHandler(this);
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "background_location");
    channel.setMethodCallHandler(new BackgroundLocationPlugin());
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("LiveTracking")) {
      String userID = call.argument("URL");
      Log.d("Tes 123", userID);
      SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);;
      SharedPreferences.Editor editor = sharedPref.edit();
      editor.putString("URL", userID);
      editor.commit();

      final Intent intent = new Intent(mContext, LiveTrackingService.class);
      mContext.startService(intent);
      mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//                            gpsService.startTracking();
//                            gpsService.startTracking();
//                            new Intent(getActivity(), gpsService.startTracking());
//                            final Intent intent2 = new Intent(getActivity(), NativeActivity.class);
//                            startActivity(intent2);
      Log.d("Tes 123", userID);
      String greetings = "Tes";
      result.success(greetings);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  private ServiceConnection serviceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      String name = className.getClassName();
      Log.d("Tes", "12345");
      if (name.endsWith("LiveTrackingService")) {
        Log.d("Tes", "12345");
        gpsService = ((LiveTrackingService.LocationServiceBinder) service).getService();
      }
    }

    public void onServiceDisconnected(ComponentName className) {
      if (className.getClassName().equals("LiveTrackingService")) {
        gpsService = null;
      }
    }
  };
}
