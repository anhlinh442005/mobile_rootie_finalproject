package com.veganbeauty.app.features.shop.barcode;

import com.veganbeauty.app.data.local.entities.ProductEntity;

public abstract class BarcodeScanState {
    private BarcodeScanState() {}

    public static final class Idle extends BarcodeScanState {
        public static final Idle INSTANCE = new Idle();
        private Idle() {}
    }

    public static final class Loading extends BarcodeScanState {
        public static final Loading INSTANCE = new Loading();
        private Loading() {}
    }

    public static final class Found extends BarcodeScanState {
        private final ProductEntity product;
        public Found(ProductEntity product) { this.product = product; }
        public ProductEntity getProduct() { return product; }
    }

    public static final class NotFound extends BarcodeScanState {
        private final String barcode;
        public NotFound(String barcode) { this.barcode = barcode; }
        public String getBarcode() { return barcode; }
    }
}
