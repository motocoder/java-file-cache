package llc.berserkr.androidfilecache;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import llc.berserkr.cache.KeyConvertingCache;
import llc.berserkr.cache.StreamFileCache;
import llc.berserkr.cache.ValueConvertingCache;
import llc.berserkr.cache.converter.BytesStringConverter;
import llc.berserkr.cache.converter.ReverseConverter;
import llc.berserkr.cache.exception.ResourceException;

public class MainActivity extends AppCompatActivity {

    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.setContentView(R.layout.activity_main);

        final FlagsView flagsView = findViewById(R.id.flags_view);

        final StreamFileCache fileCache = new StreamFileCache(getDir("image-cache", MODE_PRIVATE), 10000);

        final KeyConvertingCache<String, byte [], InputStream> keyConvertingCache =
            new KeyConvertingCache<>(
                fileCache,
                new ReverseConverter<>(new BytesStringConverter())
            );

        final Thread thread = new Thread(() -> {

            try {
                Log.d("Flags", "loading flags");

                final List<String> keys = new ArrayList<>(FlagsView.createFlagKeys());

                Log.d("Flags", "loading flags 0");

                if(!keyConvertingCache.exists("ad")) {
                    keyConvertingCache.put("ad", getDrawableInputStream(this, R.mipmap.ad));
                    keyConvertingCache.put("ae", getDrawableInputStream(this, R.mipmap.ae));
                    keyConvertingCache.put("af", getDrawableInputStream(this, R.mipmap.af));
                    keyConvertingCache.put("ag", getDrawableInputStream(this, R.mipmap.ag));
                    keyConvertingCache.put("ai", getDrawableInputStream(this, R.mipmap.ai));
                    keyConvertingCache.put("al", getDrawableInputStream(this, R.mipmap.al));
                    keyConvertingCache.put("am", getDrawableInputStream(this, R.mipmap.am));
                    keyConvertingCache.put("ao", getDrawableInputStream(this, R.mipmap.ao));
                    keyConvertingCache.put("aq", getDrawableInputStream(this, R.mipmap.aq));
                    keyConvertingCache.put("ar", getDrawableInputStream(this, R.mipmap.ar));
                    keyConvertingCache.put("as", getDrawableInputStream(this, R.mipmap.as));
                    keyConvertingCache.put("at", getDrawableInputStream(this, R.mipmap.at));
                    keyConvertingCache.put("au", getDrawableInputStream(this, R.mipmap.au));
                    keyConvertingCache.put("aw", getDrawableInputStream(this, R.mipmap.aw));
                    keyConvertingCache.put("ax", getDrawableInputStream(this, R.mipmap.ax));
                    keyConvertingCache.put("az", getDrawableInputStream(this, R.mipmap.az));
                    keyConvertingCache.put("ba", getDrawableInputStream(this, R.mipmap.ba));
                    keyConvertingCache.put("bb", getDrawableInputStream(this, R.mipmap.bb));
                    keyConvertingCache.put("bd", getDrawableInputStream(this, R.mipmap.bd));
                    keyConvertingCache.put("be", getDrawableInputStream(this, R.mipmap.be));
                    keyConvertingCache.put("bf", getDrawableInputStream(this, R.mipmap.bf));
                    keyConvertingCache.put("bg", getDrawableInputStream(this, R.mipmap.bg));
                    keyConvertingCache.put("bh", getDrawableInputStream(this, R.mipmap.bh));
                    keyConvertingCache.put("bi", getDrawableInputStream(this, R.mipmap.bi));
                    keyConvertingCache.put("bm", getDrawableInputStream(this, R.mipmap.bm));
                    keyConvertingCache.put("bn", getDrawableInputStream(this, R.mipmap.bn));
                    keyConvertingCache.put("bo", getDrawableInputStream(this, R.mipmap.bo));
                    keyConvertingCache.put("bq", getDrawableInputStream(this, R.mipmap.bq));
                    keyConvertingCache.put("br", getDrawableInputStream(this, R.mipmap.br));
                    keyConvertingCache.put("bs", getDrawableInputStream(this, R.mipmap.bs));
                    keyConvertingCache.put("bt", getDrawableInputStream(this, R.mipmap.bt));
                    keyConvertingCache.put("bv", getDrawableInputStream(this, R.mipmap.bv));
                    keyConvertingCache.put("bw", getDrawableInputStream(this, R.mipmap.bw));
                    keyConvertingCache.put("by", getDrawableInputStream(this, R.mipmap.by));
                    keyConvertingCache.put("bz", getDrawableInputStream(this, R.mipmap.bz));
                    keyConvertingCache.put("ca", getDrawableInputStream(this, R.mipmap.ca));
                    keyConvertingCache.put("cc", getDrawableInputStream(this, R.mipmap.cc));
                    keyConvertingCache.put("cd", getDrawableInputStream(this, R.mipmap.cd));
                    keyConvertingCache.put("cf", getDrawableInputStream(this, R.mipmap.cf));
                    keyConvertingCache.put("cg", getDrawableInputStream(this, R.mipmap.cg));
                    keyConvertingCache.put("ch", getDrawableInputStream(this, R.mipmap.ch));
                    keyConvertingCache.put("ci", getDrawableInputStream(this, R.mipmap.ci));
                    keyConvertingCache.put("ck", getDrawableInputStream(this, R.mipmap.ck));
                    keyConvertingCache.put("cl", getDrawableInputStream(this, R.mipmap.cl));
                    keyConvertingCache.put("cm", getDrawableInputStream(this, R.mipmap.cm));
                    keyConvertingCache.put("cn", getDrawableInputStream(this, R.mipmap.cn));
                    keyConvertingCache.put("co", getDrawableInputStream(this, R.mipmap.co));
                    keyConvertingCache.put("cr", getDrawableInputStream(this, R.mipmap.cr));
                    keyConvertingCache.put("cu", getDrawableInputStream(this, R.mipmap.cu));
                    keyConvertingCache.put("cv", getDrawableInputStream(this, R.mipmap.cv));
                    keyConvertingCache.put("cw", getDrawableInputStream(this, R.mipmap.cw));
                    keyConvertingCache.put("cx", getDrawableInputStream(this, R.mipmap.cx));
                    keyConvertingCache.put("cy", getDrawableInputStream(this, R.mipmap.cy));
                    keyConvertingCache.put("cz", getDrawableInputStream(this, R.mipmap.cz));
                    keyConvertingCache.put("de", getDrawableInputStream(this, R.mipmap.de));
                    keyConvertingCache.put("dj", getDrawableInputStream(this, R.mipmap.dj));
                    keyConvertingCache.put("dk", getDrawableInputStream(this, R.mipmap.dk));
                    keyConvertingCache.put("dm", getDrawableInputStream(this, R.mipmap.dm));
                    keyConvertingCache.put("do", getDrawableInputStream(this, R.mipmap.do_));
                    keyConvertingCache.put("dz", getDrawableInputStream(this, R.mipmap.dz));
                    keyConvertingCache.put("ec", getDrawableInputStream(this, R.mipmap.ec));
                    keyConvertingCache.put("ee", getDrawableInputStream(this, R.mipmap.ee));
                    keyConvertingCache.put("eg", getDrawableInputStream(this, R.mipmap.eg));
                    keyConvertingCache.put("eh", getDrawableInputStream(this, R.mipmap.eh));
                    keyConvertingCache.put("er", getDrawableInputStream(this, R.mipmap.er));
                    keyConvertingCache.put("es", getDrawableInputStream(this, R.mipmap.es));
                    keyConvertingCache.put("et", getDrawableInputStream(this, R.mipmap.et));
                    keyConvertingCache.put("fi", getDrawableInputStream(this, R.mipmap.fi));
                    keyConvertingCache.put("fj", getDrawableInputStream(this, R.mipmap.fj));
                    keyConvertingCache.put("fk", getDrawableInputStream(this, R.mipmap.fk));
                    keyConvertingCache.put("fm", getDrawableInputStream(this, R.mipmap.fm));
                    keyConvertingCache.put("fo", getDrawableInputStream(this, R.mipmap.fo));
                    keyConvertingCache.put("fr", getDrawableInputStream(this, R.mipmap.fr));
                    keyConvertingCache.put("ga", getDrawableInputStream(this, R.mipmap.ga));
                    keyConvertingCache.put("gb", getDrawableInputStream(this, R.mipmap.gb));
                    keyConvertingCache.put("gb_eng", getDrawableInputStream(this, R.mipmap.gb_eng));
                    keyConvertingCache.put("gb_nir", getDrawableInputStream(this, R.mipmap.gb_nir));
                    keyConvertingCache.put("gb_sct", getDrawableInputStream(this, R.mipmap.gb_sct));
                    keyConvertingCache.put("gb_wls", getDrawableInputStream(this, R.mipmap.gb_wls));
                    keyConvertingCache.put("gd", getDrawableInputStream(this, R.mipmap.gd));
                    keyConvertingCache.put("ge", getDrawableInputStream(this, R.mipmap.ge));
                    keyConvertingCache.put("gf", getDrawableInputStream(this, R.mipmap.gf));
                    keyConvertingCache.put("gg", getDrawableInputStream(this, R.mipmap.gg));
                    keyConvertingCache.put("gh", getDrawableInputStream(this, R.mipmap.gh));
                    keyConvertingCache.put("gi", getDrawableInputStream(this, R.mipmap.gi));
                    keyConvertingCache.put("gl", getDrawableInputStream(this, R.mipmap.gl));
                    keyConvertingCache.put("gm", getDrawableInputStream(this, R.mipmap.gm));
                    keyConvertingCache.put("gn", getDrawableInputStream(this, R.mipmap.gn));
                    keyConvertingCache.put("gp", getDrawableInputStream(this, R.mipmap.gp));
                    keyConvertingCache.put("gq", getDrawableInputStream(this, R.mipmap.gq));
                    keyConvertingCache.put("gr", getDrawableInputStream(this, R.mipmap.gr));
                    keyConvertingCache.put("gs", getDrawableInputStream(this, R.mipmap.gs));
                    keyConvertingCache.put("gt", getDrawableInputStream(this, R.mipmap.gt));
                    keyConvertingCache.put("gu", getDrawableInputStream(this, R.mipmap.gu));
                    keyConvertingCache.put("gw", getDrawableInputStream(this, R.mipmap.gw));
                    keyConvertingCache.put("gy", getDrawableInputStream(this, R.mipmap.gy));
                    keyConvertingCache.put("hk", getDrawableInputStream(this, R.mipmap.hk));
                    keyConvertingCache.put("hm", getDrawableInputStream(this, R.mipmap.hm));
                    keyConvertingCache.put("hn", getDrawableInputStream(this, R.mipmap.hn));
                    keyConvertingCache.put("hr", getDrawableInputStream(this, R.mipmap.hr));
                    keyConvertingCache.put("ht", getDrawableInputStream(this, R.mipmap.ht));
                    keyConvertingCache.put("hu", getDrawableInputStream(this, R.mipmap.hu));
                    keyConvertingCache.put("id", getDrawableInputStream(this, R.mipmap.id));
                    keyConvertingCache.put("ie", getDrawableInputStream(this, R.mipmap.ie));
                    keyConvertingCache.put("il", getDrawableInputStream(this, R.mipmap.il));
                    keyConvertingCache.put("im", getDrawableInputStream(this, R.mipmap.im));
                    keyConvertingCache.put("in", getDrawableInputStream(this, R.mipmap.in));
                    keyConvertingCache.put("io", getDrawableInputStream(this, R.mipmap.io));
                    keyConvertingCache.put("iq", getDrawableInputStream(this, R.mipmap.iq));
                    keyConvertingCache.put("ir", getDrawableInputStream(this, R.mipmap.ir));
                    keyConvertingCache.put("is", getDrawableInputStream(this, R.mipmap.is));
                    keyConvertingCache.put("it", getDrawableInputStream(this, R.mipmap.it));
                    keyConvertingCache.put("je", getDrawableInputStream(this, R.mipmap.je));
                    keyConvertingCache.put("jm", getDrawableInputStream(this, R.mipmap.jm));
                    keyConvertingCache.put("jo", getDrawableInputStream(this, R.mipmap.jo));
                    keyConvertingCache.put("jp", getDrawableInputStream(this, R.mipmap.jp));
                    keyConvertingCache.put("ke", getDrawableInputStream(this, R.mipmap.ke));
                    keyConvertingCache.put("kg", getDrawableInputStream(this, R.mipmap.kg));
                    keyConvertingCache.put("kh", getDrawableInputStream(this, R.mipmap.kh));
                    keyConvertingCache.put("ki", getDrawableInputStream(this, R.mipmap.ki));
                    keyConvertingCache.put("km", getDrawableInputStream(this, R.mipmap.km));
                    keyConvertingCache.put("kn", getDrawableInputStream(this, R.mipmap.kn));
                    keyConvertingCache.put("kp", getDrawableInputStream(this, R.mipmap.kp));
                    keyConvertingCache.put("kr", getDrawableInputStream(this, R.mipmap.kr));
                    keyConvertingCache.put("kw", getDrawableInputStream(this, R.mipmap.kw));
                    keyConvertingCache.put("ky", getDrawableInputStream(this, R.mipmap.ky));
                    keyConvertingCache.put("kz", getDrawableInputStream(this, R.mipmap.kz));
                    keyConvertingCache.put("la", getDrawableInputStream(this, R.mipmap.la));
                    keyConvertingCache.put("lb", getDrawableInputStream(this, R.mipmap.lb));
                    keyConvertingCache.put("lc", getDrawableInputStream(this, R.mipmap.lc));
                    keyConvertingCache.put("li", getDrawableInputStream(this, R.mipmap.li));
                    keyConvertingCache.put("lk", getDrawableInputStream(this, R.mipmap.lk));
                    keyConvertingCache.put("lr", getDrawableInputStream(this, R.mipmap.lr));
                    keyConvertingCache.put("ls", getDrawableInputStream(this, R.mipmap.ls));
                    keyConvertingCache.put("lt", getDrawableInputStream(this, R.mipmap.lt));
                    keyConvertingCache.put("lu", getDrawableInputStream(this, R.mipmap.lu));
                    keyConvertingCache.put("lv", getDrawableInputStream(this, R.mipmap.lv));
                    keyConvertingCache.put("ly", getDrawableInputStream(this, R.mipmap.ly));
                    keyConvertingCache.put("ma", getDrawableInputStream(this, R.mipmap.ma));
                    keyConvertingCache.put("mc", getDrawableInputStream(this, R.mipmap.mc));
                    keyConvertingCache.put("md", getDrawableInputStream(this, R.mipmap.md));
                    keyConvertingCache.put("me", getDrawableInputStream(this, R.mipmap.me));
                    keyConvertingCache.put("mf", getDrawableInputStream(this, R.mipmap.mf));
                    keyConvertingCache.put("mg", getDrawableInputStream(this, R.mipmap.mg));
                    keyConvertingCache.put("mh", getDrawableInputStream(this, R.mipmap.mh));
                    keyConvertingCache.put("mk", getDrawableInputStream(this, R.mipmap.mk));
                    keyConvertingCache.put("ml", getDrawableInputStream(this, R.mipmap.ml));
                    keyConvertingCache.put("mm", getDrawableInputStream(this, R.mipmap.mm));
                    keyConvertingCache.put("mn", getDrawableInputStream(this, R.mipmap.mn));
                    keyConvertingCache.put("mo", getDrawableInputStream(this, R.mipmap.mo));
                    keyConvertingCache.put("mp", getDrawableInputStream(this, R.mipmap.mp));
                    keyConvertingCache.put("mq", getDrawableInputStream(this, R.mipmap.mq));
                    keyConvertingCache.put("mr", getDrawableInputStream(this, R.mipmap.mr));
                    keyConvertingCache.put("ms", getDrawableInputStream(this, R.mipmap.ms));
                    keyConvertingCache.put("mt", getDrawableInputStream(this, R.mipmap.mt));
                    keyConvertingCache.put("mu", getDrawableInputStream(this, R.mipmap.mu));
                    keyConvertingCache.put("mv", getDrawableInputStream(this, R.mipmap.mv));
                    keyConvertingCache.put("mw", getDrawableInputStream(this, R.mipmap.mw));
                    keyConvertingCache.put("mx", getDrawableInputStream(this, R.mipmap.mx));
                    keyConvertingCache.put("my", getDrawableInputStream(this, R.mipmap.my));
                    keyConvertingCache.put("mz", getDrawableInputStream(this, R.mipmap.mz));
                    keyConvertingCache.put("na", getDrawableInputStream(this, R.mipmap.na));
                    keyConvertingCache.put("nc", getDrawableInputStream(this, R.mipmap.nc));
                    keyConvertingCache.put("ne", getDrawableInputStream(this, R.mipmap.ne));
                    keyConvertingCache.put("nf", getDrawableInputStream(this, R.mipmap.nf));
                    keyConvertingCache.put("ng", getDrawableInputStream(this, R.mipmap.ng));
                    keyConvertingCache.put("ni", getDrawableInputStream(this, R.mipmap.ni));
                    keyConvertingCache.put("nl", getDrawableInputStream(this, R.mipmap.nl));
                    keyConvertingCache.put("no", getDrawableInputStream(this, R.mipmap.no));
                    keyConvertingCache.put("np", getDrawableInputStream(this, R.mipmap.np));
                    keyConvertingCache.put("nr", getDrawableInputStream(this, R.mipmap.nr));
                    keyConvertingCache.put("nu", getDrawableInputStream(this, R.mipmap.nu));
                    keyConvertingCache.put("nz", getDrawableInputStream(this, R.mipmap.nz));
                    keyConvertingCache.put("om", getDrawableInputStream(this, R.mipmap.om));
                    keyConvertingCache.put("pa", getDrawableInputStream(this, R.mipmap.pa));
                    keyConvertingCache.put("pe", getDrawableInputStream(this, R.mipmap.pe));
                    keyConvertingCache.put("pf", getDrawableInputStream(this, R.mipmap.pf));
                    keyConvertingCache.put("pg", getDrawableInputStream(this, R.mipmap.pg));
                    keyConvertingCache.put("ph", getDrawableInputStream(this, R.mipmap.ph));
                    keyConvertingCache.put("pk", getDrawableInputStream(this, R.mipmap.pk));
                    keyConvertingCache.put("pl", getDrawableInputStream(this, R.mipmap.pl));
                    keyConvertingCache.put("pm", getDrawableInputStream(this, R.mipmap.pm));
                    keyConvertingCache.put("pn", getDrawableInputStream(this, R.mipmap.pn));
                    keyConvertingCache.put("pr", getDrawableInputStream(this, R.mipmap.pr));
                    keyConvertingCache.put("ps", getDrawableInputStream(this, R.mipmap.ps));
                    keyConvertingCache.put("pt", getDrawableInputStream(this, R.mipmap.pt));
                    keyConvertingCache.put("pw", getDrawableInputStream(this, R.mipmap.pw));
                    keyConvertingCache.put("py", getDrawableInputStream(this, R.mipmap.py));
                    keyConvertingCache.put("qa", getDrawableInputStream(this, R.mipmap.qa));
                    keyConvertingCache.put("re", getDrawableInputStream(this, R.mipmap.re));
                    keyConvertingCache.put("ro", getDrawableInputStream(this, R.mipmap.ro));
                    keyConvertingCache.put("rs", getDrawableInputStream(this, R.mipmap.rs));
                    keyConvertingCache.put("ru", getDrawableInputStream(this, R.mipmap.ru));
                    keyConvertingCache.put("rw", getDrawableInputStream(this, R.mipmap.rw));
                    keyConvertingCache.put("sa", getDrawableInputStream(this, R.mipmap.sa));
                    keyConvertingCache.put("sb", getDrawableInputStream(this, R.mipmap.sb));
                    keyConvertingCache.put("sc", getDrawableInputStream(this, R.mipmap.sc));
                    keyConvertingCache.put("sd", getDrawableInputStream(this, R.mipmap.sd));
                    keyConvertingCache.put("se", getDrawableInputStream(this, R.mipmap.se));
                    keyConvertingCache.put("sg", getDrawableInputStream(this, R.mipmap.sg));
                    keyConvertingCache.put("sh", getDrawableInputStream(this, R.mipmap.sh));
                    keyConvertingCache.put("si", getDrawableInputStream(this, R.mipmap.si));
                    keyConvertingCache.put("sj", getDrawableInputStream(this, R.mipmap.sj));
                    keyConvertingCache.put("sk", getDrawableInputStream(this, R.mipmap.sk));
                    keyConvertingCache.put("sl", getDrawableInputStream(this, R.mipmap.sl));
                    keyConvertingCache.put("sm", getDrawableInputStream(this, R.mipmap.sm));
                    keyConvertingCache.put("sn", getDrawableInputStream(this, R.mipmap.sn));
                    keyConvertingCache.put("so", getDrawableInputStream(this, R.mipmap.so));
                    keyConvertingCache.put("sr", getDrawableInputStream(this, R.mipmap.sr));
                    keyConvertingCache.put("ss", getDrawableInputStream(this, R.mipmap.ss));
                    keyConvertingCache.put("st", getDrawableInputStream(this, R.mipmap.st));
                    keyConvertingCache.put("sv", getDrawableInputStream(this, R.mipmap.sv));
                    keyConvertingCache.put("sx", getDrawableInputStream(this, R.mipmap.sx));
                    keyConvertingCache.put("sy", getDrawableInputStream(this, R.mipmap.sy));
                    keyConvertingCache.put("sz", getDrawableInputStream(this, R.mipmap.sz));
                    keyConvertingCache.put("tc", getDrawableInputStream(this, R.mipmap.tc));
                    keyConvertingCache.put("td", getDrawableInputStream(this, R.mipmap.td));
                    keyConvertingCache.put("tf", getDrawableInputStream(this, R.mipmap.tf));
                    keyConvertingCache.put("tg", getDrawableInputStream(this, R.mipmap.tg));
                    keyConvertingCache.put("th", getDrawableInputStream(this, R.mipmap.th));
                    keyConvertingCache.put("tj", getDrawableInputStream(this, R.mipmap.tj));
                    keyConvertingCache.put("tk", getDrawableInputStream(this, R.mipmap.tk));
                    keyConvertingCache.put("tl", getDrawableInputStream(this, R.mipmap.tl));
                    keyConvertingCache.put("tm", getDrawableInputStream(this, R.mipmap.tm));
                    keyConvertingCache.put("tn", getDrawableInputStream(this, R.mipmap.tn));
                    keyConvertingCache.put("to", getDrawableInputStream(this, R.mipmap.to));
                    keyConvertingCache.put("tr", getDrawableInputStream(this, R.mipmap.tr));
                    keyConvertingCache.put("tt", getDrawableInputStream(this, R.mipmap.tt));
                    keyConvertingCache.put("tv", getDrawableInputStream(this, R.mipmap.tv));
                    keyConvertingCache.put("tw", getDrawableInputStream(this, R.mipmap.tw));
                    keyConvertingCache.put("tz", getDrawableInputStream(this, R.mipmap.tz));
                    keyConvertingCache.put("ua", getDrawableInputStream(this, R.mipmap.ua));
                    keyConvertingCache.put("ug", getDrawableInputStream(this, R.mipmap.ug));
                    keyConvertingCache.put("um", getDrawableInputStream(this, R.mipmap.um));
                    keyConvertingCache.put("us", getDrawableInputStream(this, R.mipmap.us));
                    keyConvertingCache.put("uy", getDrawableInputStream(this, R.mipmap.uy));
                    keyConvertingCache.put("uz", getDrawableInputStream(this, R.mipmap.uz));
                    keyConvertingCache.put("va", getDrawableInputStream(this, R.mipmap.va));
                    keyConvertingCache.put("vc", getDrawableInputStream(this, R.mipmap.vc));
                    keyConvertingCache.put("ve", getDrawableInputStream(this, R.mipmap.ve));
                    keyConvertingCache.put("vg", getDrawableInputStream(this, R.mipmap.vg));
                    keyConvertingCache.put("vi", getDrawableInputStream(this, R.mipmap.vi));
                    keyConvertingCache.put("vn", getDrawableInputStream(this, R.mipmap.vn));
                    keyConvertingCache.put("vu", getDrawableInputStream(this, R.mipmap.vu));
                    keyConvertingCache.put("wf", getDrawableInputStream(this, R.mipmap.wf));
                    keyConvertingCache.put("ws", getDrawableInputStream(this, R.mipmap.ws));
                    keyConvertingCache.put("xk", getDrawableInputStream(this, R.mipmap.xk));
                    keyConvertingCache.put("ye", getDrawableInputStream(this, R.mipmap.ye));
                    keyConvertingCache.put("yt", getDrawableInputStream(this, R.mipmap.yt));
                    keyConvertingCache.put("za", getDrawableInputStream(this, R.mipmap.za));
                    keyConvertingCache.put("zm", getDrawableInputStream(this, R.mipmap.zm));
                    keyConvertingCache.put("zw", getDrawableInputStream(this, R.mipmap.zw));
                }

                logger.debug( "loading flags done");
                final ValueConvertingCache<String, Bitmap, InputStream> valueConvertingCache
                        = new ValueConvertingCache<>(keyConvertingCache, new InputStreamToSizedBitmapConverter(getDir("image-cache", MODE_PRIVATE), 1200, 800));

                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "loaded flags", Toast.LENGTH_SHORT).show();
                });

                ExecutorService threads = Executors.newCachedThreadPool();

                logger.debug( "kicking off flag loaders");
                for (int threadCount = 0; threadCount < 10; threadCount++) {

                    threads.execute(() -> {

                        int count = 0;

                        while (true) {
                            try {

                                final String key = flagsView.getRandomKey(keys);

                                final long startTime = System.currentTimeMillis();
                                final Bitmap bitmap = valueConvertingCache.get(key);

                                if(count++ % 100 == 0) {
                                    logger.info("loaded from cache " + (System.currentTimeMillis() - startTime));
                                }

                                if(bitmap == null) {

                                    logger.error("pushing was null " + key);
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        logger.debug("interrupted", e);
                                    }
                                    continue;
                                }

                                //threads are hitting this so fast the locks are making it laggy
                                //todo pass it in without locks or batch it or something
                                flagsView.pushThumbnail(key, bitmap);

                            } catch (ResourceException e) {
                                logger.error( "flag loader blew up.", e);
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            }
            catch (Throwable e) {
                logger.error("flag loader blew up.", e);
            }

        });

        thread.start();
    }

    /**
     * A native method that is implemented by the 'androidfilecache' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();


    public static InputStream getDrawableInputStream(Context context, int drawableResId) throws IOException {

        final Bitmap bitmap = loadBitmap(context, drawableResId);

        if(bitmap == null) {
            logger.error("bitmap is null");
            throw new IllegalStateException("bitmap null");
        }

        return bitmapToInputStream(bitmap); //holds whole image in memory be careful

    }

    private static InputStream bitmapToInputStream(Bitmap bitmap) {

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
         bitmap.recycle();

         if(byteArray == null) {
             logger.error("byteArray is null");
             throw new RuntimeException("failed to get byte array");
         }
        return new ByteArrayInputStream(byteArray);

    }

    public static Bitmap loadBitmap(Context context, int resourceID) {
        return BitmapFactory.decodeResource(context.getResources(), resourceID);
    }

}