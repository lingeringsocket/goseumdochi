// goseumdochi:  experiments with incarnation
// Copyright 2016 John V. Sichi
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.goseumdochi.android

import android._
import android.app._
import android.content._
import android.content.pm._
import android.graphics._
import android.hardware._
import android.os._
import android.util._
import android.view._
import android.widget._
import android.speech.tts._
import java.io._
import java.nio._
import java.util._
import java.util.concurrent._

import android.hardware.Camera

import scala.collection.JavaConverters._

import org.bytedeco.javacv._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._

import org.goseumdochi.common._
import org.goseumdochi.vision._
import org.goseumdochi.control._

import akka.actor._

import com.typesafe.config._

import com.orbotix._
import com.orbotix.common._

class ControlActivity extends Activity with RobotChangedStateListener
{
  private final val PERMISSION_REQUEST_CODE = 42

  private final val INITIAL_STATUS = "CONNECTED"

  private var robot : Option[ConvenienceRobot] = None

  private var wakeLock : Option[PowerManager#WakeLock] = None

  private val outputQueue =
    new java.util.concurrent.ArrayBlockingQueue[Bitmap](1)

  private val retinalInput = new AndroidRetinalInput

  private lazy val controlView =
    new ControlView(this, retinalInput, outputQueue)

  lazy val theater = new AndroidTheater(controlView, outputQueue)

  private val actuator = new AndroidSpheroActuator(this)

  private var actorSystem : Option[ActorSystem] = None

  private var textToSpeech : Option[TextToSpeech] = None

  private var controlStatus = INITIAL_STATUS

  private var lastVoiceMessage = ""

  class ControlListener extends Actor
  {
    def receive =
    {
      case ControlActor.StatusUpdateMsg(status, voiceMessage, _) => {
        controlStatus = status.toString
        speak(voiceMessage)
      }
    }
  }

  override protected def onCreate(savedInstanceState : Bundle)
  {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(savedInstanceState)

    var newTextToSpeech : TextToSpeech = null
    newTextToSpeech = new TextToSpeech(
      getApplicationContext, new TextToSpeech.OnInitListener {
        override def onInit(status : Int)
        {
          if (status != TextToSpeech.ERROR) {
            newTextToSpeech.setLanguage(Locale.UK)
            textToSpeech = Some(newTextToSpeech)
          }
          speak("Establishing Bluetooth connection.")
        }
      })

    DualStackDiscoveryAgent.getInstance.addRobotStateListener(this)
    var gotCameraPermission = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      gotCameraPermission = hasCameraPermission
      val gotLocationPermission = hasLocationPermission
      val gotPermissions = gotCameraPermission && gotLocationPermission
      if (!gotPermissions) {
        val permissions = new ArrayList[String]
        if (!gotCameraPermission) {
          permissions.add(Manifest.permission.CAMERA)
        }
        if (!gotLocationPermission) {
          permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        requestPermissions(
          permissions.toArray(
            new Array[String](permissions.size)),
          PERMISSION_REQUEST_CODE)
      }
    }
    getWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    if (gotCameraPermission) {
      startCamera
    }
  }

  private def startCamera()
  {
    val layout = new FrameLayout(this)
    val preview = new ControlPreview(this, controlView)
    layout.addView(preview)
    layout.addView(controlView)
    setContentView(layout)
    controlView.setOnTouchListener(controlView)
  }

  private def hasCameraPermission =
    hasPermission(Manifest.permission.CAMERA)

  private def hasLocationPermission =
    hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

  private def hasPermission(permission : String) =
  {
    (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) ||
      (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
  }

  override def onRequestPermissionsResult(
    requestCode : Int, permissions : Array[String], grantResults : Array[Int])
  {
    if (requestCode == PERMISSION_REQUEST_CODE) {
      for (i <- 0 until permissions.length) {
        if (grantResults(i) != PackageManager.PERMISSION_GRANTED) {
          return
        }
        permissions(i) match {
          case Manifest.permission.ACCESS_COARSE_LOCATION => startDiscovery
          case Manifest.permission.CAMERA => startCamera
          case _ =>
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  override protected def onStart()
  {
    super.onStart

    if (hasLocationPermission) {
      startDiscovery
    }
  }


  private def startDiscovery()
  {
    if(!DualStackDiscoveryAgent.getInstance.isDiscovering) {
      DualStackDiscoveryAgent.getInstance.startDiscovery(
        getApplicationContext)
    }
  }

  override protected def onStop()
  {
    if (DualStackDiscoveryAgent.getInstance.isDiscovering) {
      DualStackDiscoveryAgent.getInstance.stopDiscovery
    }

    robot.foreach(_.disconnect)
    robot = None

    super.onStop
  }

  override protected def onDestroy()
  {
    super.onDestroy
    DualStackDiscoveryAgent.getInstance.addRobotStateListener(null)
  }

  override def handleRobotChangedState(
    r : Robot,
    notification : RobotChangedStateListener.RobotChangedStateNotificationType)
  {
    if (notification ==
      RobotChangedStateListener.RobotChangedStateNotificationType.Online)
    {
      robot = Some(new ConvenienceRobot(r))
      if (actorSystem.isEmpty) {
        val file = new File(
          getApplicationContext.getFilesDir, "orientation.ser")
        System.setProperty(
          "GOSEUMDOCHI_ORIENTATION_FILE",
          file.getAbsolutePath)
        val system = ActorSystem(
          "AndroidActors",
          ConfigFactory.load("android.conf"))
        actorSystem = Some(system)
        val props = Props(
          classOf[ControlActor],
          actuator,
          Props(classOf[VisionActor], retinalInput, theater))
        val controlActor = system.actorOf(
          props, ControlActor.CONTROL_ACTOR_NAME)
        ControlActor.addListener(
          controlActor,
          system.actorOf(Props(classOf[ControlListener], this), "statusActor"))
      }
    } else {
      if (!robot.isEmpty) {
        speak("Bluetooth connection lost.")
      }
      robot = None
    }
  }

  private def acquireWakeLock()
  {
    if (wakeLock.isEmpty) {
      val pm = getSystemService(Context.POWER_SERVICE).
        asInstanceOf[PowerManager]
      val wl = pm.newWakeLock(
        PowerManager.SCREEN_DIM_WAKE_LOCK,
        getClass.getCanonicalName)
      wl.acquire
      wakeLock = Some(wl)
    }
  }

  private def releaseWakeLock()
  {
    wakeLock.foreach(wl => {
      if (wl.isHeld) {
        wl.release
      }
    })
    wakeLock = None
  }

  private def speak(voiceMessage : String)
  {
    lastVoiceMessage = voiceMessage
    textToSpeech.foreach(_.speak(voiceMessage, TextToSpeech.QUEUE_ADD, null))
  }

  override protected def onResume()
  {
    super.onResume
    acquireWakeLock
  }

  override protected def onPause()
  {
    super.onPause
    releaseWakeLock
    textToSpeech.foreach(t => {
      t.stop
      t.shutdown
    })
    textToSpeech = None
  }

  def isRobotConnected = !robot.isEmpty

  def getRobot = robot

  def getControlStatus = controlStatus

  def getVoiceMessage = lastVoiceMessage
}

class ControlView(
  context : ControlActivity,
  retinalInput : AndroidRetinalInput,
  outputQueue : ArrayBlockingQueue[Bitmap])
    extends View(context) with Camera.PreviewCallback with View.OnTouchListener
{
  private var frameNumber = 0

  override def onTouch(v : View, e : MotionEvent) =
  {
    context.theater.getVisionActor.foreach(
      _.onTheaterClick(RetinalPos(e.getX, e.getY)))
    true
  }

  override def onPreviewFrame(data : Array[Byte], camera : Camera)
  {
    try {
      if (context.isRobotConnected) {
        if (retinalInput.needsFrame) {
          val size = camera.getParameters.getPreviewSize
          val result = convertToFrame(data, size)
          retinalInput.pushFrame(result)
        }
      } else {
        postInvalidate
      }
      camera.addCallbackBuffer(data)
    } catch {
      // need to swallow these to prevent spurious crashes
      case e : RuntimeException =>
    }
  }

  private def convertToFrame(data : Array[Byte], size : Camera#Size) =
  {
    val out = new ByteArrayOutputStream
    val yuv = new YuvImage(
      data, ImageFormat.NV21, size.width, size.height, null)
    yuv.compressToJpeg(
      new android.graphics.Rect(0, 0, size.width, size.height), 50, out)
    val bytes = out.toByteArray
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length)
    val converter = new AndroidFrameConverter
    converter.convert(bitmap)
  }

  override protected def onDraw(canvas : Canvas)
  {
    if (context.isRobotConnected) {
      // janky way to hide the underlying camera preview...
      // apparently these days we should be using SurfaceTexture instead
      // (and camera2 API for that matter)
      canvas.drawARGB(255, 0, 0, 0)
    }

    val paint = new Paint
    paint.setColor(Color.RED)
    val height = 50
    paint.setTextSize(height)

    if (!outputQueue.isEmpty) {
      val bitmap = outputQueue.take
      canvas.drawBitmap(bitmap, 0, 0, paint)
    }

    val robotState = {
      if (context.isRobotConnected) {
        context.getControlStatus
      } else {
        "WAITING FOR CONNECTION"
      }
    }
    val lastVoiceMessage = context.getVoiceMessage

    canvas.drawText("Frame:  " + frameNumber, 20, 20 + height, paint)
    canvas.drawText("Sphero:  " + robotState, 20, 20 + 3*height, paint)
    canvas.drawText("Message:  " + lastVoiceMessage, 20, 20 + 5*height, paint)
    frameNumber += 1
  }
}

class ControlPreview(
  context : Context, previewCallback : Camera.PreviewCallback)
    extends SurfaceView(context) with SurfaceHolder.Callback
{
  private val surfaceHolder = createHolder
  private var camera : Option[Camera] = None

  private def createHolder() =
  {
    val holder = getHolder
    holder.addCallback(this)
    holder
  }

  override def surfaceCreated(holder : SurfaceHolder)
  {
    val newCamera = Camera.open
    camera = Some(newCamera)
    try {
      newCamera.setPreviewDisplay(holder)
    } catch {
      case exception : IOException => {
        newCamera.release
        camera = None
      }
    }
  }

  override def surfaceDestroyed(holder : SurfaceHolder)
  {
    camera.foreach(c => {
      c.stopPreview
      c.release
    })
    camera = None
  }

  override def surfaceChanged(
    holder : SurfaceHolder, format : Int,
    w : Int, h : Int)
  {
    camera.foreach(c => {
      val parameters = c.getParameters

      val sizes = parameters.getSupportedPreviewSizes.asScala
      val optimalSize = sizes.maxBy(_.height)
      parameters.setPreviewSize(optimalSize.width, optimalSize.height)
      parameters.setFocusMode("continuous-video")

      c.setParameters(parameters)
      c.setPreviewCallbackWithBuffer(previewCallback)
      val size = parameters.getPreviewSize
      val data = new Array[Byte](
        size.width * size.height *
          ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8)
      c.addCallbackBuffer(data)
      c.startPreview
    })
  }
}

class AndroidRetinalInput extends RetinalInput
{
  private val inputQueue =
    new ArrayBlockingQueue[(Frame, TimePoint)](1)

  override def nextFrame() =
  {
    inputQueue.take
  }

  override def frameToImage(frame : Frame) =
  {
    val img = super.frameToImage(frame)
    cvCvtColor(img, img, COLOR_RGBA2BGRA)
    img
  }

  def isReady() : Boolean =
  {
    !inputQueue.isEmpty
  }

  def needsFrame() : Boolean =
  {
    inputQueue.remainingCapacity > 0
  }

  def pushFrame(frame : Frame)
  {
    inputQueue.put((frame, TimePoint.now))
  }
}

class AndroidTheater(
  view : View, outputQueue : ArrayBlockingQueue[Bitmap])
    extends RetinalTheater
{
  override def imageToFrame(img : IplImage) =
  {
    cvCvtColor(img, img, COLOR_BGRA2RGBA)
    super.imageToFrame(img)
  }

  override def display(frame : Frame)
  {
    val converter = new AndroidFrameConverter
    val bitmap = converter.convert(frame)
    outputQueue.put(bitmap)
    view.postInvalidate
  }

  def getVisionActor = visionActor
}
