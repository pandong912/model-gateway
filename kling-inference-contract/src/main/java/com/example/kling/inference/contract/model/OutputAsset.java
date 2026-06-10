package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.AssetType;
import java.util.Map;

public record OutputAsset(
        String assetId,
        AssetType type,
        String url,
        String mimeType,
        Integer width,
        Integer height,
        Integer durationSeconds,
        Map<String, Object> metadata
) {
}
