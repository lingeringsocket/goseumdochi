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

package org.goseumdochi.android.leash;

// adapted from https://github.com/jaredcorso/Pandamation

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class EfficientAnimation {
    public static class EaFrame {
        byte[] bytes;
        Drawable drawable;
        boolean isReady = false;
    }

    public interface OnDrawableLoadedListener {
        void onDrawableLoaded(List<EaFrame> frames);
    }

    public static void loadRaw(final int resourceId, final Context context, final OnDrawableLoadedListener onDrawableLoadedListener) {
        loadFromXml(resourceId, context, onDrawableLoadedListener);
    }

    private static void loadFromXml(final int resourceId, final Context context, final OnDrawableLoadedListener onDrawableLoadedListener) {
        new Thread(new Runnable() {
                @Override
                public void run() {
                    final ArrayList<EaFrame> frames = new ArrayList<>();

                    XmlResourceParser parser = context.getResources().getXml(resourceId);

                    try {
                        int eventType = parser.getEventType();
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_DOCUMENT) {

                            } else if (eventType == XmlPullParser.START_TAG) {

                                if (parser.getName().equals("item")) {
                                    byte[] bytes = null;

                                    for (int i=0; i<parser.getAttributeCount(); i++) {
                                        String attrName = parser.getAttributeName(i);
                                        if (attrName.endsWith("drawable")) {
                                            int resId = Integer.parseInt(parser.getAttributeValue(i).substring(1));
                                            bytes = IOUtils.toByteArray(context.getResources().openRawResource(resId));
                                        }
                                    }

                                    EaFrame frame = new EaFrame();
                                    frame.bytes = bytes;
                                    frames.add(frame);
                                }

                            } else if (eventType == XmlPullParser.END_TAG) {

                            } else if (eventType == XmlPullParser.TEXT) {

                            }

                            eventType = parser.next();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Run on UI Thread
                    new Handler(context.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (onDrawableLoadedListener != null) {
                                    onDrawableLoadedListener.onDrawableLoaded(frames);
                                }
                            }
                        });
                }
            }).run();
    }

    public static void animate(int resourceId, final ImageView imageView, final int duration) {
        loadRaw(resourceId, imageView.getContext(), new OnDrawableLoadedListener() {
                @Override
                public void onDrawableLoaded(List<EaFrame> frames) {
                    animate(frames, imageView, 0, true, duration);
                }
            });
    }

    private static void animate(final List<EaFrame> frames, final ImageView imageView, final int frameNumber, final boolean first, final int duration) {
        final EaFrame thisFrame = frames.get(frameNumber);

        if (first) {
            thisFrame.drawable = new BitmapDrawable(imageView.getContext().getResources(), BitmapFactory.decodeByteArray(thisFrame.bytes, 0, thisFrame.bytes.length));
        }
        else {
            int prevFrameNumber;
            if (frameNumber == 0) {
                prevFrameNumber = frames.size() - 1;
            } else {
                prevFrameNumber = frameNumber - 1;
            }
            EaFrame previousFrame = frames.get(prevFrameNumber);
            ((BitmapDrawable) previousFrame.drawable).getBitmap().recycle();
            previousFrame.drawable = null;
            previousFrame.isReady = false;
        }

        final int nextFrameNumber = (frameNumber >= (frames.size() - 1)) ? 0 :
            frameNumber + 1;
        imageView.setImageDrawable(thisFrame.drawable);
        new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (imageView.getDrawable() == thisFrame.drawable) {
                        EaFrame nextFrame = frames.get(nextFrameNumber);
                        if (nextFrame.isReady) {
                            animate(frames, imageView, nextFrameNumber, false, duration);
                        }
                        else {
                            nextFrame.isReady = true;
                        }
                    }
                }
            }, duration);

        new Thread(new Runnable() {
                @Override
                public void run() {
                    EaFrame nextFrame = frames.get(nextFrameNumber);
                    nextFrame.drawable = new BitmapDrawable(imageView.getContext().getResources(), BitmapFactory.decodeByteArray(nextFrame.bytes, 0, nextFrame.bytes.length));
                    if (nextFrame.isReady) {
                        animate(frames, imageView, nextFrameNumber, false, duration);
                    }
                    else {
                        nextFrame.isReady = true;
                    }

                }
            }).run();
    }
}
