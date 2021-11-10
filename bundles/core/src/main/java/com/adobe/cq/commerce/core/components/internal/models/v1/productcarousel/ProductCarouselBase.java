package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.io.IOException;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ProductCarouselBase extends DataLayerComponent {

    @JsonSerialize(as = CommerceIdentifier.class)
    protected class CommerceIdentifierImpl extends DataLayerListItem implements CommerceIdentifier {

        private final String sku;

        public CommerceIdentifierImpl(String sku) {
            super(ProductCarouselBase.this.getId(), null);
            this.sku = sku;
        }

        @Override
        protected String getIdentifier() {
            return getValue();
        }

        @Override
        public String getValue() {
            return sku;
        }

        @Override
        public IdentifierType getType() {
            return IdentifierType.SKU;
        }

        @Override
        public EntityType getEntityType() {
            return EntityType.PRODUCT;
        }
    }

    public static class CommerceIdentifierImplSerializer extends JsonSerializer<CommerceIdentifierImpl> {

        @Override
        public void serialize(CommerceIdentifierImpl commerceIdentifier, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("id", commerceIdentifier.getId());
            jsonGenerator.writeObjectFieldStart("commerceIdentifier");
            jsonGenerator.writeStringField("entityType", commerceIdentifier.getEntityType().toString());
            jsonGenerator.writeStringField("value", commerceIdentifier.getValue());
            jsonGenerator.writeStringField("type", commerceIdentifier.getType().toString());
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }
    }
}
