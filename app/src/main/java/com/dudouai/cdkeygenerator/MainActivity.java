package com.dudouai.cdkeygenerator;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * DUDOU AI CDKey 生成器 — Android 版
 * 界面: 极简商务风格
 * 算法与桌面端 (cdkey-gen.py / main.js) 完全兼容
 */
public class MainActivity extends AppCompatActivity {

    // ⚠️ 必须与桌面端 main.js 中的 LICENSE_SECRET 完全一致
    private static final String LICENSE_SECRET = "DudouAI_License_Secret_Key_2026_V1";

    // 授权类型映射（与桌面端 TYPE_MAP 一致）
    private static final int[][] LICENSE_TYPES = {
        {1, 30},     // 月卡
        {2, 365},    // 年卡
        {3, 65535},  // 终生授权
    };

    // ── 视图引用 ──
    private LinearLayout cardMonthly, cardYearly, cardLifetime;
    private TextView btnMinus, btnPlus, tvMaxActivations;
    private TextView chip1, chip5, chip10, chip20;
    private Button generateBtn;
    private TextView btnClear, btnCopy;
    private TextView resultText, infoText;

    // ── 当前选中状态 ──
    private int selectedTypeIdx = 0;      // 0=月卡, 1=年卡, 2=终生
    private int maxActivations = 2;       // 最大激活数
    private int generateCount = 1;        // 生成数量
    private String generatedKeys = "";    // 当前结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 绑定视图
        cardMonthly = findViewById(R.id.cardMonthly);
        cardYearly = findViewById(R.id.cardYearly);
        cardLifetime = findViewById(R.id.cardLifetime);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);
        tvMaxActivations = findViewById(R.id.tvMaxActivations);
        chip1 = findViewById(R.id.chip1);
        chip5 = findViewById(R.id.chip5);
        chip10 = findViewById(R.id.chip10);
        chip20 = findViewById(R.id.chip20);
        generateBtn = findViewById(R.id.generateBtn);
        btnClear = findViewById(R.id.btnClear);
        btnCopy = findViewById(R.id.btnCopy);
        resultText = findViewById(R.id.resultText);
        infoText = findViewById(R.id.infoText);

        // ── 授权类型卡片选择 ──
        updateCardSelection();
        cardMonthly.setOnClickListener(v -> { selectedTypeIdx = 0; updateCardSelection(); });
        cardYearly.setOnClickListener(v -> { selectedTypeIdx = 1; updateCardSelection(); });
        cardLifetime.setOnClickListener(v -> { selectedTypeIdx = 2; updateCardSelection(); });

        // ── 最大激活数 +/- 按钮 ──
        updateActivationsDisplay();
        btnMinus.setOnClickListener(v -> {
            if (maxActivations > 0) { maxActivations--; updateActivationsDisplay(); }
        });
        btnPlus.setOnClickListener(v -> {
            if (maxActivations < 254) { maxActivations++; updateActivationsDisplay(); }
        });

        // ── 生成数量标签切换 ──
        updateCountChips();
        chip1.setOnClickListener(v -> { generateCount = 1; updateCountChips(); });
        chip5.setOnClickListener(v -> { generateCount = 5; updateCountChips(); });
        chip10.setOnClickListener(v -> { generateCount = 10; updateCountChips(); });
        chip20.setOnClickListener(v -> { generateCount = 20; updateCountChips(); });

        // ── 生成按钮 ──
        generateBtn.setOnClickListener(v -> generateCDKeys());

        // ── 清空按钮 ──
        btnClear.setOnClickListener(v -> {
            generatedKeys = "";
            resultText.setText("点击「生成 CDKey」按钮开始");
            infoText.setText("算法: HMAC-SHA256 | 与桌面端完全兼容");
        });

        // ── 复制按钮 ──
        btnCopy.setOnClickListener(v -> {
            if (generatedKeys.isEmpty()) {
                infoText.setText("⚠️ 没有可复制的内容，请先生成 CDKey");
                return;
            }
            android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip =
                android.content.ClipData.newPlainText("CDKey", generatedKeys);
            clipboard.setPrimaryClip(clip);
            infoText.setText("✅ 已复制 " + generateCount + " 个 CDKey 到剪贴板");
        });
    }

    // ═══════════════════════════════════════════════
    //   UI 状态更新
    // ═══════════════════════════════════════════════

    private void updateCardSelection() {
        cardMonthly.setSelected(selectedTypeIdx == 0);
        cardYearly.setSelected(selectedTypeIdx == 1);
        cardLifetime.setSelected(selectedTypeIdx == 2);

        // 更新卡片内数字颜色
        updateCardTextColor(cardMonthly, selectedTypeIdx == 0);
        updateCardTextColor(cardYearly, selectedTypeIdx == 1);
        updateCardTextColor(cardLifetime, selectedTypeIdx == 2);
    }

    private void updateCardTextColor(LinearLayout card, boolean selected) {
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(
                    selected ? 0xFFa855f7 : 0xFF999999);
            }
        }
    }

    private void updateActivationsDisplay() {
        if (maxActivations == 0) {
            tvMaxActivations.setText("无限制");
        } else {
            tvMaxActivations.setText(String.valueOf(maxActivations));
        }
    }

    private void updateCountChips() {
        resetChip(chip1, generateCount == 1);
        resetChip(chip5, generateCount == 5);
        resetChip(chip10, generateCount == 10);
        resetChip(chip20, generateCount == 20);
    }

    private void resetChip(TextView chip, boolean selected) {
        chip.setTextColor(selected ? 0xFFa78bfa : 0xFF666666);
        chip.setBackgroundColor(selected ? 0xFF1a1a2e : 0xFF1a1a1a);
    }

    // ═══════════════════════════════════════════════
    //   CDKey 生成算法（与桌面端完全一致）
    // ═══════════════════════════════════════════════

    /** 密钥派生：SHA256(secret) → 32字节密钥 */
    private byte[] deriveKey(String secret) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成单个 CDKey */
    private String generateCDKey(int typeId, int days, int maxActivations) throws Exception {
        // 1. 构建 9 字节 payload
        byte[] payload = new byte[9];
        payload[0] = (byte) typeId;

        // 天数 (大端序 uint16)
        payload[1] = (byte) ((days >> 8) & 0xFF);
        payload[2] = (byte) (days & 0xFF);

        // Unix 时间戳 (大端序 uint32)
        int now = (int) (System.currentTimeMillis() / 1000);
        payload[3] = (byte) ((now >> 24) & 0xFF);
        payload[4] = (byte) ((now >> 16) & 0xFF);
        payload[5] = (byte) ((now >> 8) & 0xFF);
        payload[6] = (byte) (now & 0xFF);

        // 随机数 nonce
        payload[7] = (byte) (new SecureRandom().nextInt(256));

        // 最大激活数（0=无限制→255, 否则 1-254）
        if (maxActivations <= 0) {
            payload[8] = (byte) 255;
        } else {
            int clamped = Math.max(1, Math.min(maxActivations, 254));
            payload[8] = (byte) clamped;
        }

        // 2. HMAC-SHA256 签名
        byte[] derivedKey = deriveKey(LICENSE_SECRET);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(derivedKey, "HmacSHA256"));
        byte[] fullSig = mac.doFinal(payload);

        // 取前 8 字节
        byte[] sig = new byte[8];
        System.arraycopy(fullSig, 0, sig, 0, 8);

        // 3. 拼接 raw = payload(9) + sig(8) = 17 字节
        byte[] raw = new byte[17];
        System.arraycopy(payload, 0, raw, 0, 9);
        System.arraycopy(sig, 0, raw, 9, 8);

        // 4. 转十六进制大写
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            hex.append(String.format("%02X", b));
        }

        // 5. 格式化为 DDAI-XXXX-XXXX-... (34 hex → 9 组)
        String hexStr = hex.toString();
        StringBuilder formatted = new StringBuilder("DDAI-");
        for (int i = 0; i < hexStr.length(); i += 4) {
            if (i > 0) formatted.append('-');
            int end = Math.min(i + 4, hexStr.length());
            formatted.append(hexStr, i, end);
        }

        return formatted.toString();
    }

    /** 处理生成按钮点击 */
    private void generateCDKeys() {
        try {
            int typeId = LICENSE_TYPES[selectedTypeIdx][0];
            int days = LICENSE_TYPES[selectedTypeIdx][1];

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < generateCount; i++) {
                String cdkey = generateCDKey(typeId, days, maxActivations);
                sb.append(cdkey);
                if (i < generateCount - 1) sb.append('\n');
            }

            generatedKeys = sb.toString();
            resultText.setText(generatedKeys);

            // 自动复制第一条
            String firstKey = generatedKeys.split("\n")[0];
            android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText("CDKey", firstKey));

            infoText.setText("✅ 已生成 " + generateCount + " 个 CDKey | 首条已复制到剪贴板");

        } catch (Exception e) {
            infoText.setText("❌ 生成失败: " + e.getMessage());
        }
    }
}
