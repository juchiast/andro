package andro.id.caroboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLSurfaceView extends GLSurfaceView {
    private final MyGLRenderer renderer;
    private final Context context;
    private final SM stateMachine;

    public MyGLSurfaceView(Context context) {
        this(context, null);
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);
        this.context = context;
        renderer = new MyGLRenderer(context);
        setRenderer(renderer);
        stateMachine = new SM(context, renderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        stateMachine.drive(event);
        return true;
    }

    void onClick(int x, int y) {
        MainActivity.callback.call(x, y, this);
    }

    public void setBoard(int x, int y, int p) {
        renderer.setBoard(x, y, p);
    }

    interface SMNode {
        SMNode up(MotionEvent event);

        SMNode down(MotionEvent event);

        SMNode move(MotionEvent event);
    }

    class SM {
        private final Context context;
        private final MyGLRenderer renderer;
        private SMNode current;

        SM(Context context, MyGLRenderer renderer) {
            this.context = context;
            this.renderer = renderer;
            this.current = new NoTouch();
        }

        void drive(MotionEvent event) {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    current = current.down(event);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    current = current.up(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    current = current.move(event);
                    break;
            }
        }

        class Tracker {
            private long time;
            private long startTime;
            private float x, y;
            final float startX, startY;

            private float velocityX;
            private float velocityY;

            Tracker(float x, float y) {
                time = System.currentTimeMillis();
                this.x = x;
                this.y = y;
                velocityX = 0;
                velocityY = 0;

                this.startX = x;
                this.startY = y;
                this.startTime = time;
            }

            void move(float x, float y) {
                long dt = System.currentTimeMillis() - this.time;

                velocityX = (x - this.x) / dt;
                velocityY = (y - this.y) / dt;

                this.time += dt;
                this.x = x;
                this.y = y;
            }

            void up(float x, float y) {
                this.time = System.currentTimeMillis();
                this.x = x;
                this.y = y;
            }

            float getVelocityX() {
                return velocityX;
            }

            float getVelocityY() {
                return velocityY;
            }

            boolean isClick() {
                return Math.abs(x - startX) <= 15 && Math.abs(y - startY) <= 15 && (time - startTime) <= 150;
            }

            float getX() {
                return x;
            }

            float getY() {
                return y;
            }
        }

        class MultiTouch implements SMNode {
            Tracker[] trackers;
            float x, y;
            long time;

            MultiTouch(MotionEvent event) {
                trackers = new Tracker[2];
                time = System.currentTimeMillis();
                trackers[0] = new Tracker(event.getX(0), event.getY(0));
                trackers[1] = new Tracker(event.getX(1), event.getY(1));

                x = trackers[1].getX() - trackers[0].getX();
                y = trackers[1].getY() - trackers[0].getY();
            }

            @Override
            public SMNode up(MotionEvent event) {
                renderer.setVelocity(0, 0, 0, 0, 0);
                return new NoTouch();
            }

            @Override
            public SMNode down(MotionEvent event) {
                return this;
            }

            @Override
            public SMNode move(MotionEvent event) {
                long dt = System.currentTimeMillis() - time;
                trackers[0].move(event.getX(0), event.getY(0));
                trackers[1].move(event.getX(1), event.getY(1));

                float vX = (trackers[1].getVelocityX() + trackers[0].getVelocityX()) / 2;
                float vY = (trackers[1].getVelocityY() + trackers[0].getVelocityY()) / 2;

                float x = trackers[1].getX() - trackers[0].getX();
                float y = trackers[1].getY() - trackers[0].getY();

                float d1 = (float) Math.sqrt(x * x + y * y);
                float d0 = (float) Math.sqrt(this.x * this.x + this.y * this.y);

                float vD = -(d1 - d0) / dt;

                float th0 = (float) Math.acos(this.x / d0);
                if (this.y < 0) {
                    th0 = -th0;
                }
                float th1 = (float) Math.acos(x / d1);
                if (y < 0) {
                    th1 = -th1;
                }
                float vTh = (th1 - th0) / dt;

                renderer.setVelocity(vX, vY, vD, vTh, 0);

                this.x = x;
                this.y = y;
                this.time += dt;

                return this;
            }
        }

        class SingleTouch implements SMNode {
            Tracker tracker;

            SingleTouch(MotionEvent event) {
                tracker = new Tracker(event.getX(), event.getY());
            }

            @Override
            public SMNode up(MotionEvent event) {
                tracker.up(event.getX(0), event.getY(0));
                if (tracker.isClick()) {
                    int[] r = new int[2];
                    renderer.unProject(tracker.startX, tracker.startY, r);
                    if (r[0] >= 0 && r[1] >= 0) {
                        onClick(r[0], r[1]);
                    }
                } else {
                    renderer.setVelocity(tracker.getVelocityX(), tracker.getVelocityY(), 0, 0, 0);
                    renderer.setAccelerate(-tracker.getVelocityX(), -tracker.getVelocityY());
                }
                return new NoTouch();
            }

            @Override
            public SMNode down(MotionEvent event) {
                return new MultiTouch(event);
            }

            @Override
            public SMNode move(MotionEvent event) {
                tracker.move(event.getX(0), event.getY(0));
                renderer.setVelocity(tracker.getVelocityX(), tracker.getVelocityY(), 0, 0, 0);
                return this;
            }
        }

        class NoTouch implements SMNode {

            @Override
            public SMNode up(MotionEvent event) {
                return this;
            }

            @Override
            public SMNode down(MotionEvent event) {
                return new SingleTouch(event);
            }

            @Override
            public SMNode move(MotionEvent event) {
                return this;
            }
        }
    }

    class MyGLRenderer implements GLSurfaceView.Renderer {
        void setBoard(int x, int y, int p) {
            if (0 <= p && p <= 2) {
                board[y * SIZE + x] = (byte) p;
            }
        }

        class Eye {
            private float x, y;
            private float d;
            private float th;
            private float z;

            Eye() {
                x = 0;
                y = 0;
                d = 70;
                th = (float) Math.PI / 2;
                z = (float) Math.PI / 4;
            }

            void applyVelocity(float vX, float vY, float vD, float vTh, float vZ, long dt) {
                d += vD * dt / 8;
                if (d > 100) d = 100;
                if (d < 10) d = 10;
                x += (Math.cos(th - Math.PI / 2) * -vX - Math.sin(th - Math.PI / 2) * -vY) / 90 * dt;
                y += (Math.sin(th - Math.PI / 2) * -vX + Math.cos(th - Math.PI / 2) * -vY) / 90 * dt;
                if (x < -SIZE / 2) x = -SIZE / 2;
                if (x > SIZE / 2) x = SIZE / 2;
                if (y < -SIZE / 2) y = -SIZE / 2;
                if (y > SIZE / 2) y = SIZE / 2;
                th -= vTh * dt;
                z += vZ * dt;
            }

            void getViewMatrix(float[] mat) {
                float centerX = x;
                float centerY = y;
                float centerZ = 0;
                float upX = (float) Math.cos(th);
                float upY = (float) Math.sin(th);
                float upZ = 0;
                float eyeX = centerX + d * upX * (float) Math.cos(z);
                float eyeY = centerY + d * upY * (float) Math.cos(z);
                float eyeZ = centerZ + d * (float) Math.sin(z);
                Matrix.setLookAtM(mat, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
            }
        }

        // 20 * 20 board
        //
        // 0:    c0.png
        // 1: o  c1.png
        // 2: x  c2.png
        private final int SIZE = 20;
        private final byte[] board = new byte[SIZE * SIZE];

        private Eye eye;
        private final Context context;

        private final int[] textures = new int[3];
        private int program;

        private final float[] projectionMatrix = new float[16];
        private final float[] viewMatrix = new float[16];
        private final int[] viewport = new int[4];

        void unProject(float cx, float cy, int[] r) {
            float[] obj = new float[3];

            int res = -1;
            float minDist = 1e9f;
            for (int i = 0; i < SIZE * SIZE; i++) {
                int x = i % SIZE - SIZE / 2;
                int y = i / SIZE - SIZE / 2;
                GLU.gluProject(x + 0.5f, y + 0.5f, 0, viewMatrix, 0, projectionMatrix, 0, viewport, 0, obj, 0);
                obj[1] = viewport[3] - obj[1];
                float dist = (obj[0] - cx) * (obj[0] - cx) + (obj[1] - cy) * (obj[1] - cy);

                if (dist < minDist) {
                    res = i;
                    minDist = dist;
                }
            }
            r[0] = res % SIZE;
            r[1] = res / SIZE;
        }

        MyGLRenderer(Context context) {
            this.context = context;
            this.eye = new Eye();
        }

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
            int vertex = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER);
            GLES30.glShaderSource(vertex, getResourceString(context, R.raw.vertex));
            GLES30.glCompileShader(vertex);
            int fragment = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER);
            GLES30.glShaderSource(fragment, getResourceString(context, R.raw.fragment));
            GLES30.glCompileShader(fragment);
            int program = GLES30.glCreateProgram();
            GLES30.glAttachShader(program, vertex);
            GLES30.glAttachShader(program, fragment);
            GLES30.glLinkProgram(program);
            GLES30.glUseProgram(program);

            int[] buffers = new int[1];
            GLES30.glGenBuffers(1, buffers, 0);
            float[] data = new float[]{
                    -1, 1, 1, 1, 1, -1,
                    -1, 1, 1, -1, -1, -1
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(12 * 4);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(data);
            fb.position(0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 12 * 4, fb, GLES30.GL_STATIC_DRAW);
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, 0);


            this.program = program;

            GLES30.glGenTextures(3, textures, 0);
            loadTexture(textures[0], R.drawable.c0);
            loadTexture(textures[1], R.drawable.c1);
            loadTexture(textures[2], R.drawable.c2);

            eye.getViewMatrix(viewMatrix);
            int view = GLES30.glGetUniformLocation(program, "view");
            GLES30.glUniformMatrix4fv(view, 1, false, viewMatrix, 0);
        }


        private float vX = 0, vY = 0, vD = 0, vTh = 0, vZ = 0;

        void setVelocity(float x, float y, float d, float th, float z) {
            vX = x;
            vY = y;
            vD = d;
            vTh = th;
            vZ = z;
            accelerateX = 0;
            accelerateY = 0;
        }

        private float accelerateX = 0;
        private float accelerateY = 0;

        void setAccelerate(float x, float y) {
            accelerateX = x;
            accelerateY = y;
        }

        private void loadTexture(int texture, int resId) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture);
            Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resId);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bm, 0);
            bm.recycle();
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            GLES30.glViewport(0, 0, width, height);
            viewport[0] = 0;
            viewport[1] = 1;
            viewport[2] = width;
            viewport[3] = height;
            float ratio = (float) width / height;
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, 1, -1, 3, 1000);
            int projection = GLES30.glGetUniformLocation(this.program, "projection");
            GLES30.glUniformMatrix4fv(projection, 1, false, projectionMatrix, 0);
        }

        private long time = 0;

        @Override
        public void onDrawFrame(GL10 unused) {
            if (time == 0) {
                time = System.currentTimeMillis();
            } else {
                long dt = System.currentTimeMillis() - time;
                eye.applyVelocity(vX, vY, vD, vTh, vZ, dt);
                eye.getViewMatrix(viewMatrix);
                int view = GLES30.glGetUniformLocation(program, "view");
                GLES30.glUniformMatrix4fv(view, 1, false, viewMatrix, 0);
                time += dt;
                vX += accelerateX * dt / 1000;
                vY += accelerateY * dt / 1000;
                if (vX * accelerateX >= 0) vX = accelerateX = 0;
                if (vY * accelerateY >= 0) vY = accelerateY = 0;
            }

            GLES30.glClearColor(1, 1, 1, 1);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

            int model = GLES30.glGetUniformLocation(this.program, "model");
            for (int i = 0; i < SIZE * SIZE; i++) {
                int x = i % SIZE - SIZE / 2;
                int y = i / SIZE - SIZE / 2;
                GLES30.glUniform2f(model, x, y);
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[board[i]]);
                GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);
            }
        }
    }

    static String getResourceString(Context context, int id) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(id)));
        StringBuilder text = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}
