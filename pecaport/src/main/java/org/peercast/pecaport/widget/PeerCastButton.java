package org.peercast.pecaport.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

/**
 * peercast_button.xml内で使用。
 */
public class PeerCastButton extends FrameLayout {
    /**
     * メガフォンのアイコンを持つボタン<br>
     * duplicateParentState="true"でボタンのクリック感は残す。
     */
    private ImageButton vMegaphone;
    //Deny状態を示す赤いバツ印
    private ImageView vClose;

    private static final float ALPHA_DISABLED_ICON = 0.7f;

    public PeerCastButton(Context context) {
        super(context);
    }

    public PeerCastButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PeerCastButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        vMegaphone = (ImageButton) findViewById(android.R.id.button1);
        vClose = (ImageView) findViewById(android.R.id.icon1);

        if (vMegaphone == null || vClose == null)
            throw new NullPointerException();
    }

    /**
     * クリックイベントを子に渡さない。
     * @return always true
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }


    /***/
    public boolean isDeny() {
        return vClose.getVisibility() == VISIBLE;
    }

    /**
     * 赤いバツ印を表示する/しない
     *
     * @see #isDeny()
     */
    public void setDeny(boolean b) {
        vClose.setVisibility(b ? VISIBLE : INVISIBLE);
    }

    /**
     * falseのとき、半透明にする。
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        vMegaphone.setEnabled(enabled);
        float alpha = enabled ? 1 : ALPHA_DISABLED_ICON;
        vMegaphone.setAlpha(alpha);
        vClose.setAlpha(alpha);
    }
}
