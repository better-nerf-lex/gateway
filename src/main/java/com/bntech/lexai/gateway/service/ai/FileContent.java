package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.data.object.StorageFileType;

public record FileContent(String name, byte[] content, StorageFileType fileType) {
}
