package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.AssetType;
import java.util.Map;

public record InputAsset(
        String assetId,
        AssetType type,
        String url,
        String base64,
        String mimeType,
        Map<String, Object> metadata
) {
}
