package arun.com.chromer.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import arun.com.chromer.shared.Constants;

/**
 * Created by Arun on 12/06/2016.
 */
public class ColorUtil {

    public final static int[] ACCENT_COLORS = new int[]{
            Color.parseColor("#FF1744"),
            Color.parseColor("#F50057"),
            Color.parseColor("#D500F9"),
            Color.parseColor("#651FFF"),
            Color.parseColor("#3D5AFE"),
            Color.parseColor("#2979FF"),
            Color.parseColor("#00B0FF"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#1DE9B6"),
            Color.parseColor("#00E676"),
            Color.parseColor("#76FF03"),
            Color.parseColor("#C6FF00"),
            Color.parseColor("#FFEA00"),
            Color.parseColor("#FFC400"),
            Color.parseColor("#FF9100"),
            Color.parseColor("#FF3D00")
    };

    public final static int[] ACCENT_COLORS_700 = new int[]{
            Color.parseColor("#D32F2F"),
            Color.parseColor("#C2185B"),
            Color.parseColor("#7B1FA2"),
            Color.parseColor("#6200EA"),
            Color.parseColor("#304FFE"),
            Color.parseColor("#2962FF"),
            Color.parseColor("#0091EA"),
            Color.parseColor("#00B8D4"),
            Color.parseColor("#00BFA5"),
            Color.parseColor("#00C853"),
            Color.parseColor("#64DD17"),
            Color.parseColor("#AEEA00"),
            Color.parseColor("#FFD600"),
            Color.parseColor("#FFAB00"),
            Color.parseColor("#FF6D00"),
            Color.parseColor("#DD2C00"),
            Color.parseColor("#455A64")
    };

    @NonNull
    public static List<Palette.Swatch> getSwatchListFromPalette(@NonNull Palette palette) {
        final List<Palette.Swatch> swatchList = new ArrayList<>();

        Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
        Palette.Swatch vibrantDarkSwatch = palette.getDarkVibrantSwatch();
        Palette.Swatch vibrantLightSwatch = palette.getLightVibrantSwatch();
        Palette.Swatch mutedSwatch = palette.getMutedSwatch();
        Palette.Swatch mutedDarkSwatch = palette.getDarkMutedSwatch();
        Palette.Swatch mutedLightSwatch = palette.getLightMutedSwatch();

        swatchList.add(vibrantSwatch);
        swatchList.add(vibrantDarkSwatch);
        swatchList.add(vibrantLightSwatch);
        swatchList.add(mutedSwatch);
        swatchList.add(mutedDarkSwatch);
        swatchList.add(mutedLightSwatch);
        return swatchList;
    }

    public static double colorDifference(@ColorInt int a, @ColorInt int b) {
        double aLab[] = new double[3];
        double bLab[] = new double[3];
        ColorUtils.colorToLAB(a, aLab);
        ColorUtils.colorToLAB(b, bLab);
        return ColorUtils.distanceEuclidean(aLab, bLab);
    }

    @ColorInt
    public static int getClosestAccentColor(@ColorInt int color) {
        final SortedMap<Double, Integer> set = new TreeMap<>();
        color = (0xFFFFFF - color) | 0xFF000000;
        for (int i = 0; i < ACCENT_COLORS_700.length; i++) {
            set.put(colorDifference(color, ACCENT_COLORS_700[i]), i);
        }
        return ACCENT_COLORS_700[set.get(set.firstKey())];
    }

    @ColorInt
    public static int getBestFaviconColor(@Nullable Palette palette) {
        if (palette != null) {
            List<Palette.Swatch> sortedSwatch = getSwatchListFromPalette(palette);
            // Descending
            Collections.sort(sortedSwatch,
                    new Comparator<Palette.Swatch>() {
                        @Override
                        public int compare(Palette.Swatch swatch1, Palette.Swatch swatch2) {
                            int a = swatch1 == null ? 0 : swatch1.getPopulation();
                            int b = swatch2 == null ? 0 : swatch2.getPopulation();
                            return b - a;
                        }
                    });

            // We want the vibrant color but we will avoid it if it is the most prominent one.
            // Instead we will choose the next prominent color
            int vibrantColor = palette.getVibrantColor(Constants.NO_COLOR);
            int prominentColor = sortedSwatch.get(0).getRgb();
            if (vibrantColor == Constants.NO_COLOR) {
                int darkVibrantColor = palette.getDarkVibrantColor(Constants.NO_COLOR);
                if (darkVibrantColor != Constants.NO_COLOR) {
                    return darkVibrantColor;
                } else {
                    int mutedColor = palette.getMutedColor(Constants.NO_COLOR);
                    if (mutedColor != Constants.NO_COLOR) {
                        return mutedColor;
                    } else {
                        return prominentColor;
                    }
                }
            } else return vibrantColor;
        }
        return Constants.NO_COLOR;
    }

    /*@ColorInt
    public static int getForegroundTextColor(@ColorInt int backgroundColor) {
        final int whiteColorAlpha = ColorUtils.calculateMinimumAlpha(Color.WHITE, backgroundColor, 4.5f);

        if (whiteColorAlpha != -1) {
            return ColorUtils.setAlphaComponent(Color.WHITE, whiteColorAlpha);
        }

        final int blackColorAlpha = ColorUtils.calculateMinimumAlpha(Color.BLACK, backgroundColor, 4.5f);

        if (blackColorAlpha != -1) {
            return ColorUtils.setAlphaComponent(Color.BLACK, blackColorAlpha);
        }

        //noinspection ConstantConditions
        return whiteColorAlpha != -1 ? ColorUtils.setAlphaComponent(Color.WHITE, whiteColorAlpha)
                : ColorUtils.setAlphaComponent(Color.BLACK, blackColorAlpha);
    }*/

    /**
     * Returns white or black based on color luminance
     *
     * @param backgroundColor the color to get foreground for
     * @return White for darker colors and black for ligher colors
     */
    @ColorInt
    public static int getForegroundWhiteOrBlack(@ColorInt int backgroundColor) {
        float hsl[] = new float[3];
        ColorUtils.colorToHSL(backgroundColor, hsl);
        float l = hsl[2];
        if (l < .5) {
            return Color.WHITE;
        } else
            return Color.BLACK;
    }


    @ColorInt
    public static int getBestColorFromPalette(@Nullable Palette palette) {
        if (palette == null) {
            return Constants.NO_COLOR;
        }
        int vibrantColor = palette.getVibrantColor(Constants.NO_COLOR);
        if (vibrantColor != Constants.NO_COLOR) {
            return vibrantColor;
        } else {
            int darkVibrantColor = palette.getDarkVibrantColor(Constants.NO_COLOR);
            if (darkVibrantColor != Constants.NO_COLOR) {
                return darkVibrantColor;
            } else {
                return palette.getDarkMutedColor(Constants.NO_COLOR);
            }
        }
    }

    @NonNull
    public static Drawable getRippleDrawableCompat(final @ColorInt int color) {
        if (Util.isLollipopAbove()) {
            return new RippleDrawable(ColorStateList.valueOf(color),
                    null,
                    null
            );
        }
        int translucentColor = ColorUtils.setAlphaComponent(color, 0x44);
        StateListDrawable stateListDrawable = new StateListDrawable();
        int[] states = new int[]{android.R.attr.state_pressed};
        stateListDrawable.addState(states, new ColorDrawable(translucentColor));
        return stateListDrawable;
    }
}
