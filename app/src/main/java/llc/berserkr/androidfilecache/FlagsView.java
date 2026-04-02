package llc.berserkr.androidfilecache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class FlagsView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    //#################### SurfaceView mechanics ####################
    private final SurfaceHolder holder;
    private boolean running = false;

    //######################
    private int w;
    private int h;
    private int itemWidth;
    private int itemHeight;
    private Paint apparentIndicatorPaint;
//    private FlagLoader flagLoader = (code, loaded) -> {
//        /* stub */
//    };

    /**
     * these constructors are defined to use this view in the xml file
     * @param context
     */
    public FlagsView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);

        init(context);

    }

    /**
     * these constructors are defined to use this view in the xml file
     *
     * @param context
     * @param attrs
     */
    public FlagsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        holder = getHolder();
        holder.addCallback(this);

        init(context);
    }

    /**
     * these constructors are defined to use this view in the xml file
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public FlagsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        holder = getHolder();
        holder.addCallback(this);

        init(context);
    }

    /**
     * these constructors are defined to use this view in the xml file
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     * @param defStyleRes
     */
    public FlagsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        holder = getHolder();
        holder.addCallback(this);

        init(context);

    }

    private List<String> keys = new ArrayList<>();

    private void init(Context context) {

        this.keys.addAll(createFlagKeys());

        apparentIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        apparentIndicatorPaint.setStyle(Paint.Style.FILL);
        apparentIndicatorPaint.setColor(0xDDE38900);


    }

    public static Collection<String> createFlagKeys() {

        final List<String> keys = new ArrayList<>();

        keys.add("ad");
        keys.add("ae");
        keys.add("af");
        keys.add("ag");
        keys.add("ai");
        keys.add("al");
        keys.add("am");
        keys.add("ao");
        keys.add("aq");
        keys.add("ar");
        keys.add("as");
        keys.add("at");
        keys.add("au");
        keys.add("aw");
        keys.add("ax");
        keys.add("az");
        keys.add("ba");
        keys.add("bb");
        keys.add("bd");
        keys.add("be");
        keys.add("bf");
        keys.add("bg");
        keys.add("bh");
        keys.add("bi");
        keys.add("bm");
        keys.add("bn");
        keys.add("bo");
        keys.add("bq");
        keys.add("br");
        keys.add("bs");
        keys.add("bt");
        keys.add("bv");
        keys.add("bw");
        keys.add("by");
        keys.add("bz");
        keys.add("ca");
        keys.add("cc");
        keys.add("cd");
        keys.add("cf");
        keys.add("cg");
        keys.add("ch");
        keys.add("ci");
        keys.add("ck");
        keys.add("cl");
        keys.add("cm");
        keys.add("cn");
        keys.add("co");
        keys.add("cr");
        keys.add("cu");
        keys.add("cv");
        keys.add("cw");
        keys.add("cx");
        keys.add("cy");
        keys.add("cz");
        keys.add("de");
        keys.add("dj");
        keys.add("dk");
        keys.add("dm");
        keys.add("do");
        keys.add("dz");
        keys.add("ec");
        keys.add("ee");
        keys.add("eg");
        keys.add("eh");
        keys.add("er");
        keys.add("es");
        keys.add("et");
        keys.add("fi");
        keys.add("fj");
        keys.add("fk");
        keys.add("fm");
        keys.add("fo");
        keys.add("fr");
        keys.add("ga");
        keys.add("gb");
        keys.add("gb_eng");
        keys.add("gb_nir");
        keys.add("gb_sct");
        keys.add("gb_wls");
        keys.add("gd");
        keys.add("ge");
        keys.add("gf");
        keys.add("gg");
        keys.add("gh");
        keys.add("gi");
        keys.add("gl");
        keys.add("gm");
        keys.add("gn");
        keys.add("gp");
        keys.add("gq");
        keys.add("gr");
        keys.add("gs");
        keys.add("gt");
        keys.add("gu");
        keys.add("gw");
        keys.add("gy");
        keys.add("hk");
        keys.add("hm");
        keys.add("hn");
        keys.add("hr");
        keys.add("ht");
        keys.add("hu");
        keys.add("id");
        keys.add("ie");
        keys.add("il");
        keys.add("im");
        keys.add("in");
        keys.add("io");
        keys.add("iq");
        keys.add("ir");
        keys.add("is");
        keys.add("it");
        keys.add("je");
        keys.add("jm");
        keys.add("jo");
        keys.add("jp");
        keys.add("ke");
        keys.add("kg");
        keys.add("kh");
        keys.add("ki");
        keys.add("km");
        keys.add("kn");
        keys.add("kp");
        keys.add("kr");
        keys.add("kw");
        keys.add("ky");
        keys.add("kz");
        keys.add("la");
        keys.add("lb");
        keys.add("lc");
        keys.add("li");
        keys.add("lk");
        keys.add("lr");
        keys.add("ls");
        keys.add("lt");
        keys.add("lu");
        keys.add("lv");
        keys.add("ly");
        keys.add("ma");
        keys.add("mc");
        keys.add("md");
        keys.add("me");
        keys.add("mf");
        keys.add("mg");
        keys.add("mh");
        keys.add("mk");
        keys.add("ml");
        keys.add("mm");
        keys.add("mn");
        keys.add("mo");
        keys.add("mp");
        keys.add("mq");
        keys.add("mr");
        keys.add("ms");
        keys.add("mt");
        keys.add("mu");
        keys.add("mv");
        keys.add("mw");
        keys.add("mx");
        keys.add("my");
        keys.add("mz");
        keys.add("na");
        keys.add("nc");
        keys.add("ne");
        keys.add("nf");
        keys.add("ng");
        keys.add("ni");
        keys.add("nl");
        keys.add("no");
        keys.add("np");
        keys.add("nr");
        keys.add("nu");
        keys.add("nz");
        keys.add("om");
        keys.add("pa");
        keys.add("pe");
        keys.add("pf");
        keys.add("pg");
        keys.add("ph");
        keys.add("pk");
        keys.add("pl");
        keys.add("pm");
        keys.add("pn");
        keys.add("pr");
        keys.add("ps");
        keys.add("pt");
        keys.add("pw");
        keys.add("py");
        keys.add("qa");
        keys.add("re");
        keys.add("ro");
        keys.add("rs");
        keys.add("ru");
        keys.add("rw");
        keys.add("sa");
        keys.add("sb");
        keys.add("sc");
        keys.add("sd");
        keys.add("se");
        keys.add("sg");
        keys.add("sh");
        keys.add("si");
        keys.add("sj");
        keys.add("sk");
        keys.add("sl");
        keys.add("sm");
        keys.add("sn");
        keys.add("so");
        keys.add("sr");
        keys.add("ss");
        keys.add("st");
        keys.add("sv");
        keys.add("sx");
        keys.add("sy");
        keys.add("sz");
        keys.add("tc");
        keys.add("td");
        keys.add("tf");
        keys.add("tg");
        keys.add("th");
        keys.add("tj");
        keys.add("tk");
        keys.add("tl");
        keys.add("tm");
        keys.add("tn");
        keys.add("to");
        keys.add("tr");
        keys.add("tt");
        keys.add("tv");
        keys.add("tw");
        keys.add("tz");
        keys.add("ua");
        keys.add("ug");
        keys.add("um");
        keys.add("us");
        keys.add("uy");
        keys.add("uz");
        keys.add("va");
        keys.add("vc");
        keys.add("ve");
        keys.add("vg");
        keys.add("vi");
        keys.add("vn");
        keys.add("vu");
        keys.add("wf");
        keys.add("ws");
        keys.add("xk");
        keys.add("ye");
        keys.add("yt");
        keys.add("za");
        keys.add("zm");
        keys.add("zw");

        return keys;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        Thread renderThread = new Thread(this);
        renderThread.start();

        takeMeasurements(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        // Handle surface changes (e.g., orientation changes)
        takeMeasurements(width, height);

    }

    public String getRandomKey(final List<String> keys) {
        final int randomIndex = (int) (Math.random() * keys.size());
        return keys.get(randomIndex);
    }

    private final Map<String, String> cellMap = new HashMap<>();
    private final Map<String, Bitmap> thumbnailMap = new ConcurrentHashMap<>();
    private final Map<String, Long> thumbnailUpdate = new ConcurrentHashMap<>();

    public void pushThumbnail(String key, Bitmap thumbnail) {
        thumbnailMap.put(key, thumbnail);
        thumbnailUpdate.put(key, System.currentTimeMillis());
    }

    private void takeMeasurements(int width, int height) {

        this.w = width;
        this.h = height;

        //1280x896

        itemWidth = (int)(w * 0.05F);
        itemHeight = 896 * itemWidth / 1280;

        final List<String> sessionKeys = new ArrayList<>(keys);

        final int cols = w / itemWidth;

        final int rows = h / itemHeight;

        for(int c = 1; c <= cols; c++) {
            for(int r = 1; r <= rows; r++) {

                cellMap.put(c + "_" + r, getRandomKey(sessionKeys));

            }
        }

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        running = false;
    }

    @Override
    public void run() {
        while (running) {

            if (!holder.getSurface().isValid()) {
                continue;
            }

            final Canvas canvas = holder.lockCanvas();
            if (canvas != null) {

                // Clear the canvas (optional, but often necessary for full redraw)
                canvas.drawColor(Color.BLACK); // Or any background color

                final int rows = h / itemHeight;
                final int cols = w / itemWidth;

                for(int c = 1; c <= cols; c++) {
                    for (int r = 1; r <= rows; r++) {
                        canvas.save();

                        canvas.translate(c * itemWidth, r * itemHeight);

                        final String cell = cellMap.get(c + "_" + r);

                        Bitmap thumbnail = null;

                        if(cell != null) {

                            thumbnail = thumbnailMap.get(cell);
                        }

                        if (thumbnail != null) {

                            Long update = thumbnailUpdate.get(cell);

                            if(update == null) {
                                update = 0L;
                            }

                            int alpha = (int) ((Math.min(System.currentTimeMillis() - update, 1000) / 1000f) * 255);

                            final Paint paint = new Paint();
                            paint.setAntiAlias(true);

                            RectF destRect = new RectF(0, 0, itemWidth, itemHeight);
                            Rect srcRect = new Rect(0, 0, thumbnail.getWidth(), thumbnail.getHeight()); // Source rectangle in the bitmap

                            canvas.drawBitmap(thumbnail, srcRect, destRect, paint);

                            apparentIndicatorPaint.setAlpha(alpha);
                            canvas.drawRect(destRect, apparentIndicatorPaint);
                        }

                        canvas.restore();

                    }
                }

                // Perform drawing operations on the canvas
                holder.unlockCanvasAndPost(canvas);

            }
        }
    }


    /**
     * This returns the pixels for the dp value
     *
     * @param dp
     * @param context
     * @return
     */
    public static int dpToPx(float dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

//    private void setFlagLoader(FlagLoader flagLoader) {
//        this.flagLoader = flagLoader;
//    }
//
//    public interface FlagLoader {
//
//        void loadFlag(final String code, Consumer<Bitmap> loaded);
//    }

}
