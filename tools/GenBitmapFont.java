import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter;
import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Noto Sans JP サブセット TTF から BitmapFont（.fnt + .png）を生成するツール。
 * <p>
 * 背景：gdx-freetype は HTML5（GWT）で動かないため、Web版では事前に生成した
 * ビットマップフォントを使う。生成には同じ FreeType ラスタライザと
 * 同じサイズ（32px / 24px）を使うので、描画結果は従来と変わらない。
 * <p>
 * 使い方（リポジトリルートから）:
 * <pre>
 *   javac -encoding UTF-8 -cp &lt;gdx jars&gt; -d /tmp/genfont tools/GenBitmapFont.java
 *   java -cp /tmp/genfont:&lt;gdx jars&gt; GenBitmapFont \
 *       android/assets/fonts/NotoSansJP-subset.ttf tools/chars.txt \
 *       android/assets/fonts 32
 * </pre>
 * 出力：android/assets/fonts/font32.fnt + font32.png（サイズごとに1組）。
 * <p>
 * 文字セットは tools/chars.txt（tools/subset_font.py が core/src/*.java から
 * 収集したもの）。新しい日本語文字列を追加した場合は、まず
 * tools/subset_font.py で TTF と chars.txt を更新し、このツールを再実行する。
 */
public class GenBitmapFont {
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("usage: GenBitmapFont <ttf> <chars.txt> <outDir> <size>");
            System.exit(2);
        }
        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    installMockGL();
                    run(args[0], args[1], args[2], Integer.parseInt(args[3]));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                System.exit(0);
            }
        });
    }

    /**
     * HeadlessApplication は Gdx.gl を設定しないため、Texture 生成（PixmapPacker
     * がフォントアトラスのテクスチャを作る箇所）で NPE になる。GL20 の全メソッドを
     * 何もしないスタブで埋める。テクスチャの中身は使わず、Pixmap だけが要る。
     */
    private static void installMockGL() {
        GL20 gl = (GL20) Proxy.newProxyInstance(GenBitmapFont.class.getClassLoader(),
                new Class<?>[] {GL20.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        Class<?> r = method.getReturnType();
                        if (r == int.class) return 1;
                        if (r == boolean.class) return Boolean.FALSE;
                        if (r == float.class) return 0f;
                        if (r == String.class) return "";
                        return null;
                    }
                });
        Gdx.gl = gl;
        Gdx.gl20 = gl;
    }

    private static void run(String ttfPath, String charsPath, String outDir, int size) throws Exception {
        String raw = new String(Files.readAllBytes(new File(charsPath).toPath()), StandardCharsets.UTF_8);
        Set<Character> unique = new LinkedHashSet<Character>();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\n' || c == '\r') continue;
            unique.add(c);
        }
        StringBuilder sb = new StringBuilder(unique.size());
        for (Character c : unique) sb.append(c.charValue());
        String characters = sb.toString();

        FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(new FileHandle(new File(ttfPath)));
        FreeTypeFontGenerator.FreeTypeFontParameter params =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = size;
        params.characters = characters;
        params.packer = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 2, false);
        BitmapFont font = generator.generateFont(params);

        Array<PixmapPacker.Page> pages = params.packer.getPages();
        Pixmap[] pixmaps = new Pixmap[pages.size];
        for (int i = 0; i < pages.size; i++) {
            pixmaps[i] = pages.get(i).getPixmap();
        }
        int glyphs = 0;
        for (int i = 0; i < font.getData().glyphs.length; i++) {
            if (font.getData().glyphs[i] == null) continue;
            for (int j = 0; j < font.getData().glyphs[i].length; j++) {
                if (font.getData().glyphs[i][j] != null) glyphs++;
            }
        }
        File outFile = new File(outDir, "font" + size + ".fnt");
        BitmapFontWriter.writeFont(font.getData(), pixmaps, new FileHandle(outFile),
                new BitmapFontWriter.FontInfo("NotoSansJP", size));
        System.out.println("size=" + size + " chars=" + characters.length()
                + " glyphs=" + glyphs + " pages=" + pixmaps.length
                + " wrote " + outFile.getPath());
        generator.dispose();
    }
}
