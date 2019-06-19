package cn.cbdi.savecap;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;

public class ChooseWindow extends PopupWindow implements View.OnClickListener {

    private View mContentView;
    private Activity mActivity;
    OptionTypeListener listener;

    Button btn_call;

    Button btn_PersonOption;

    Button btn_change;

    public ChooseWindow(Activity activity) {
        mActivity = activity;
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mContentView = LayoutInflater.from(activity).inflate(R.layout.layout_choose_option, null);
        setContentView(mContentView);
        btn_call = (Button) mContentView.findViewById(R.id.btn_call);
        btn_PersonOption = (Button) mContentView.findViewById(R.id.btn_PersonOption);
        btn_change = (Button) mContentView.findViewById(R.id.btn_change);
        btn_call.setOnClickListener(this);
        btn_PersonOption.setOnClickListener(this);
        btn_change.setOnClickListener(this);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00000000")));
        setAnimationStyle(R.style.Person_type_Popup);
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss() {
                lighton();
            }
        });
    }


    private void lighton() {
        WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();
        lp.alpha = 1.0f;
        mActivity.getWindow().setAttributes(lp);
    }

    private void lightoff() {
        WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();

        lp.alpha = 0.3f;
        mActivity.getWindow().setAttributes(lp);
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        lightoff();
        super.showAsDropDown(anchor, xoff, yoff);
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        lightoff();
        super.showAtLocation(parent, gravity, x, y);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_call:
                listener.onOptionType(btn_call, 1);
                break;
            case R.id.btn_PersonOption:
                listener.onOptionType(btn_PersonOption, 2);
                break;
            case R.id.btn_change:
                listener.onOptionType(btn_PersonOption, 3);
                break;
        }
    }

    public interface OptionTypeListener {
        void onOptionType(Button view, int type);
    }

    public void setOptionTypeListener(OptionTypeListener listener) {
        this.listener = listener;
    }

}
