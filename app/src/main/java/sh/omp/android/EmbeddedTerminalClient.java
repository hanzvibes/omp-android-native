package sh.omp.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

public final class EmbeddedTerminalClient implements TerminalSessionClient, TerminalViewClient {
    private final Context context;
    private TerminalView terminalView;

    public EmbeddedTerminalClient(Context context) {
        this.context = context;
    }

    public void attachTerminalView(TerminalView view) {
        terminalView = view;
    }

    @Override public void onTextChanged(TerminalSession session) {
        if (terminalView != null) terminalView.postInvalidate();
    }
    @Override public void onTitleChanged(TerminalSession session) {
        if (terminalView != null) terminalView.postInvalidate();
    }
    @Override public void onSessionFinished(TerminalSession session) {
        if (terminalView != null) terminalView.postInvalidate();
    }
    @Override public void onCopyTextToClipboard(TerminalSession session, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("terminal", text));
    }
    @Override public void onPasteTextFromClipboard(TerminalSession session) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            CharSequence value = clipboard.getPrimaryClip().getItemAt(0).coerceToText(context);
            if (value != null && session.getEmulator() != null) session.getEmulator().paste(value.toString());
        }
    }
    @Override public void onBell(TerminalSession session) { }
    @Override public void onColorsChanged(TerminalSession session) {
        if (terminalView != null) terminalView.postInvalidate();
    }
    @Override public void onTerminalCursorStateChange(boolean state) {
        if (terminalView != null) terminalView.postInvalidate();
    }
    @Override public Integer getTerminalCursorStyle() { return null; }

    @Override public float onScale(float scale) { return Math.max(0.7f, Math.min(scale, 2.0f)); }
    @Override public void onSingleTapUp(MotionEvent event) {
        if (terminalView != null) {
            terminalView.requestFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    @Override public boolean shouldBackButtonBeMappedToEscape() { return false; }
    @Override public boolean shouldEnforceCharBasedInput() { return true; }
    @Override public boolean shouldUseCtrlSpaceWorkaround() { return false; }
    @Override public boolean isTerminalViewSelected() { return true; }
    @Override public void copyModeChanged(boolean copyMode) { }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event, TerminalSession session) { return false; }
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) { return false; }
    @Override public boolean onLongPress(MotionEvent event) { return false; }
    @Override public boolean readControlKey() { return false; }
    @Override public boolean readAltKey() { return false; }
    @Override public boolean readShiftKey() { return false; }
    @Override public boolean readFnKey() { return false; }
    @Override public boolean onCodePoint(int codePoint, boolean controlDown, TerminalSession session) { return false; }
    @Override public void onEmulatorSet() { }

    @Override public void logError(String tag, String message) { android.util.Log.e(tag, message); }
    @Override public void logWarn(String tag, String message) { android.util.Log.w(tag, message); }
    @Override public void logInfo(String tag, String message) { android.util.Log.i(tag, message); }
    @Override public void logDebug(String tag, String message) { android.util.Log.d(tag, message); }
    @Override public void logVerbose(String tag, String message) { android.util.Log.v(tag, message); }
    @Override public void logStackTraceWithMessage(String tag, String message, Exception error) { android.util.Log.e(tag, message, error); }
    @Override public void logStackTrace(String tag, Exception error) { android.util.Log.e(tag, "Terminal error", error); }
}