package com.bidscube.sdk.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Test utility for windowed ad positioning and layout testing
 */
public class WindowedAdTest {
    
    private static final String TAG = "WindowedAdTest";
    
    /**
     * Test windowed ad positioning in a scroll view with header, footer, and sidebar
     * @param context The application context
     * @return The test layout containing all components
     */
    public static View createWindowedAdTestLayout(Context context) {
        SDKLogger.d(TAG, "Creating windowed ad test layout...");

        FrameLayout mainContainer = new FrameLayout(context);
        mainContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        mainContainer.setBackgroundColor(Color.WHITE);

        View header = createHeader(context);
        mainContainer.addView(header);

        View footer = createFooter(context);
        mainContainer.addView(footer);

        View sidebar = createSidebar(context);
        mainContainer.addView(sidebar);

        View mainContent = createMainContentWithScroll(context);
        mainContainer.addView(mainContent);
        
        SDKLogger.d(TAG, "Windowed ad test layout created successfully");
        return mainContainer;
    }
    
    /**
     * Create header attached to top of current window
     */
    private static View createHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#2196F3"));
        header.setPadding(16, 16, 16, 16);

        TextView headerText = new TextView(context);
        headerText.setText("HEADER - Attached to Top of Current Window");
        headerText.setTextColor(Color.WHITE);
        headerText.setTextSize(16);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        header.addView(headerText);
        
        SDKLogger.d(TAG, "Header created - attached to top of current window");
        return header;
    }
    
    /**
     * Create footer attached to bottom of current window
     */
    private static View createFooter(Context context) {
        LinearLayout footer = new LinearLayout(context);
        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        footerParams.gravity = Gravity.BOTTOM;
        footer.setLayoutParams(footerParams);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setBackgroundColor(Color.parseColor("#FF5722"));
        footer.setPadding(16, 16, 16, 16);

        TextView footerText = new TextView(context);
        footerText.setText("FOOTER - Attached to Bottom of Current Window");
        footerText.setTextColor(Color.WHITE);
        footerText.setTextSize(16);
        footerText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        footer.addView(footerText);
        
        SDKLogger.d(TAG, "Footer created - attached to bottom of current window");
        return footer;
    }
    
    /**
     * Create sidebar attached to left side
     */
    private static View createSidebar(Context context) {
        LinearLayout sidebar = new LinearLayout(context);
        FrameLayout.LayoutParams sidebarParams = new FrameLayout.LayoutParams(
            200,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        sidebarParams.gravity = Gravity.LEFT;
        sidebarParams.topMargin = 80;
        sidebarParams.bottomMargin = 80;
        sidebar.setLayoutParams(sidebarParams);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackgroundColor(Color.parseColor("#4CAF50"));
        sidebar.setPadding(16, 16, 16, 16);

        TextView sidebarTitle = new TextView(context);
        sidebarTitle.setText("SIDEBAR");
        sidebarTitle.setTextColor(Color.WHITE);
        sidebarTitle.setTextSize(18);
        sidebarTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        sidebarTitle.setGravity(Gravity.CENTER);
        sidebar.addView(sidebarTitle);

        TextView sidebarContent = new TextView(context);
        sidebarContent.setText("Attached to Left Side\n\nFixed Width: 200dp\n\nDoesn't influence above/below fold");
        sidebarContent.setTextColor(Color.WHITE);
        sidebarContent.setTextSize(14);
        sidebarContent.setGravity(Gravity.CENTER);
        sidebarContent.setPadding(0, 20, 0, 0);
        sidebar.addView(sidebarContent);
        
        SDKLogger.d(TAG, "Sidebar created - attached to left side");
        return sidebar;
    }
    
    /**
     * Create main content area with scroll view
     */
    private static View createMainContentWithScroll(Context context) {

        FrameLayout mainContent = new FrameLayout(context);
        FrameLayout.LayoutParams mainContentParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        mainContentParams.leftMargin = 200;
        mainContentParams.topMargin = 80;
        mainContentParams.bottomMargin = 80;
        mainContent.setLayoutParams(mainContentParams);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout content = new LinearLayout(context);
        content.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 16, 16, 16);

        addTestContentSection(content, context, "Above the Fold Content", 
            "This content is visible without scrolling. It should not be influenced by windowed ads.", 
            Color.parseColor("#E3F2FD"));
        
        addTestContentSection(content, context, "Windowed Ad Test Area", 
            "This area is designed for testing windowed ads. The ad should be positioned here without affecting above/below fold visibility.", 
            Color.parseColor("#FFF3E0"));
        
        addTestContentSection(content, context, "Below the Fold Content", 
            "This content requires scrolling to see. Windowed ads should not interfere with this content's visibility.", 
            Color.parseColor("#F3E5F5"));

        for (int i = 1; i <= 10; i++) {
            addTestContentSection(content, context, "Additional Content Section " + i, 
                "This is additional content to ensure the scroll view has enough content to scroll. " +
                "Windowed ads should be positioned correctly regardless of scroll position.", 
                Color.parseColor("#E8F5E8"));
        }
        
        scrollView.addView(content);
        mainContent.addView(scrollView);
        
        SDKLogger.d(TAG, "Main content with scroll view created");
        return mainContent;
    }
    
    /**
     * Add a test content section
     */
    private static void addTestContentSection(LinearLayout parent, Context context, String title, String description, int backgroundColor) {
        LinearLayout section = new LinearLayout(context);
        section.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(backgroundColor);
        section.setPadding(16, 16, 16, 16);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(Color.BLACK);
        titleView.setTextSize(18);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 8);
        section.addView(titleView);

        TextView descView = new TextView(context);
        descView.setText(description);
        descView.setTextColor(Color.BLACK);
        descView.setTextSize(14);
        descView.setPadding(0, 0, 0, 16);
        section.addView(descView);
        
        parent.addView(section);
    }
    
    /**
     * Test windowed ad positioning in the layout
     * @param layout The layout to test
     * @param adView The ad view to position
     * @param position The desired position (TOP, CENTER, BOTTOM, LEFT, RIGHT)
     */
    public static void testWindowedAdPositioning(View layout, View adView, String position) {
        SDKLogger.d(TAG, "Testing windowed ad positioning: " + position);
        
        if (layout instanceof FrameLayout) {
            FrameLayout frameLayout = (FrameLayout) layout;

            for (int i = 0; i < frameLayout.getChildCount(); i++) {
                View child = frameLayout.getChildAt(i);
                if (child.getTag() != null && "WINDOWED_AD".equals(child.getTag())) {
                    frameLayout.removeView(child);
                    break;
                }
            }

            FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            
            switch (position.toUpperCase()) {
                case "TOP":
                    adParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    adParams.topMargin = 100;
                    break;
                    
                case "CENTER":
                    adParams.gravity = Gravity.CENTER;
                    break;
                    
                case "BOTTOM":
                    adParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    adParams.bottomMargin = 100;
                    break;
                    
                case "LEFT":
                    adParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                    adParams.leftMargin = 220;
                    break;
                    
                case "RIGHT":
                    adParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                    adParams.rightMargin = 16;
                    break;
                    
                default:
                    adParams.gravity = Gravity.CENTER;
                    break;
            }
            
            adView.setLayoutParams(adParams);
            adView.setTag("WINDOWED_AD");

            frameLayout.addView(adView);
            
            SDKLogger.d(TAG, "Windowed ad positioned at: " + position);
            
        } else {
            SDKLogger.e(TAG, "Layout is not a FrameLayout, cannot position ad");
        }
    }
    
    /**
     * Test scroll behavior with windowed ads
     * @param layout The layout to test
     */
    public static void testScrollBehavior(View layout) {
        SDKLogger.d(TAG, "Testing scroll behavior with windowed ads...");

        ScrollView scrollView = findScrollView(layout);
        if (scrollView != null) {
            SDKLogger.d(TAG, "ScrollView found - testing scroll behavior");

            scrollView.post(() -> {

                scrollView.smoothScrollTo(0, scrollView.getChildAt(0).getHeight() / 2);
                SDKLogger.d(TAG, "Scrolled to middle position");

                scrollView.postDelayed(() -> {
                    scrollView.smoothScrollTo(0, scrollView.getChildAt(0).getHeight());
                    SDKLogger.d(TAG, "Scrolled to bottom position");
                }, 2000);

                scrollView.postDelayed(() -> {
                    scrollView.smoothScrollTo(0, 0);
                    SDKLogger.d(TAG, "Scrolled to top position");
                }, 4000);
            });
            
        } else {
            SDKLogger.e(TAG, "ScrollView not found in layout");
        }
    }
    
    /**
     * Find ScrollView in the layout hierarchy
     */
    private static ScrollView findScrollView(View view) {
        if (view instanceof ScrollView) {
            return (ScrollView) view;
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                ScrollView found = findScrollView(viewGroup.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validate windowed ad positioning requirements
     * @param layout The layout to validate
     * @return true if all requirements are met
     */
    public static boolean validateWindowedAdRequirements(View layout) {
        SDKLogger.d(TAG, "Validating windowed ad positioning requirements...");
        
        boolean isValid = true;

        if (!hasHeader(layout)) {
            SDKLogger.e(TAG, "✗ Missing header attached to top of current window");
            isValid = false;
        } else {
            SDKLogger.d(TAG, "✓ Header found - attached to top of current window");
        }

        if (!hasFooter(layout)) {
            SDKLogger.e(TAG, "✗ Missing footer attached to bottom of current window");
            isValid = false;
        } else {
            SDKLogger.d(TAG, "✓ Footer found - attached to bottom of current window");
        }

        if (!hasSidebar(layout)) {
            SDKLogger.e(TAG, "✗ Missing sidebar attached to left/right side");
            isValid = false;
        } else {
            SDKLogger.d(TAG, "✓ Sidebar found - attached to left side");
        }

        if (!hasScrollView(layout)) {
            SDKLogger.e(TAG, "✗ Missing scroll view for content");
            isValid = false;
        } else {
            SDKLogger.d(TAG, "✓ Scroll view found - content can scroll");
        }

        if (!(layout instanceof FrameLayout)) {
            SDKLogger.e(TAG, "✗ Layout must be FrameLayout for proper windowed ad positioning");
            isValid = false;
        } else {
            SDKLogger.d(TAG, "✓ Layout is FrameLayout - proper positioning supported");
        }
        
        if (isValid) {
            SDKLogger.d(TAG, "✓ All windowed ad positioning requirements met!");
        } else {
            SDKLogger.e(TAG, "✗ Some windowed ad positioning requirements not met");
        }
        
        return isValid;
    }
    
    private static boolean hasHeader(View view) {
        return findViewByText(view, "HEADER") != null;
    }
    
    private static boolean hasFooter(View view) {
        return findViewByText(view, "FOOTER") != null;
    }
    
    private static boolean hasSidebar(View view) {
        return findViewByText(view, "SIDEBAR") != null;
    }
    
    private static boolean hasScrollView(View view) {
        return findScrollView(view) != null;
    }
    
    private static View findViewByText(View view, String text) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (textView.getText().toString().contains(text)) {
                return textView;
            }
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View found = findViewByText(viewGroup.getChildAt(i), text);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
}

