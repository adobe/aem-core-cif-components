# DRAFT: Customizing CIF Core Components

This document describes customization of CIF Core Components on the layers of Sling models and data retrieval via GraphQL. For all other customizations, please follow the patters described in [Customizing Core Components](https://docs.adobe.com/content/help/en/experience-manager-core-components/using/developing/customizing.html).


## Use Cases

1. **Use Case A**

    Change of logic inside the component.

    *Example: Change the formatting of a product price.*

1. **Use Case B**

    Use Magento GraphQL schema to query one or more additional properties and display them.

    *Example: Display a custom product property.*

1. **Use Case C**

    Use a custom GraphQL schema that is a superset of the Magento GraphQL schema to create a custom query to implement and extend the component.

    *Example: Include a custom inventory system in your schema to display additional inventory information that are not present in the Magento schema.*

1. **Use Case D**

    Use a custom GraphQL schema that is fully custom and not a superset of the Magento GraphQL schema to create a custom query to implement and extend the component.

    *Example: Include a custom order management system in your schema that replaces the existing order management schema of Magento and both APIs are not compatible.*

## Customization Layers
1. Sightly Templates
1. Sling Model Interface
1. Sling Model Implementation
1. Data Layer (GraphQL Query)
1. GraphQL Schema (Model Classes)

## Use Case to Layer Mapping

| Use Case | Template | Interface | Implementation | Data Layer | Schema |
| -------- | -------- | --------- | -------------- | ---------- | ------ |
| A        | ✔️       | ✔️        | 🔶            | ✔️          | ✔️    |
| B        | 🔶       | ❌        | 🔶            | 🔶          | ✔️    |
| C        | 🔶       | ❌        | 🔶            | ❌          | ❌    |
| D        | ❌       | ❌        | ❌            | ❌          | ❌    |

✔️ no adaption needed, ❌ adaption needed, 🔶 partial re-use possible

## Examples

### Use Case A
Please refer to [Delegation Pattern for Sling Models](https://github.com/adobe/aem-core-wcm-components/wiki/Delegation-Pattern-for-Sling-Models).

### Use Case B

#### Custom Sling Model
* Interface (`com.{YOUR_PROJECT}.cif.core.models.MyProduct`)
    ```java
    package com.venia.cif.core.models;

    import com.adobe.cq.commerce.core.components.models.product.Product;
    import com.shopify.graphql.support.SchemaViolationError;
    import org.osgi.annotation.versioning.ProviderType;

    @ProviderType
    public interface MyProduct extends Product {
        // Extend the existing interface with the additional properties which you
        // want to expose to the HTL template.
        public String getCreatedAt();
        public String isReturnable() throws SchemaViolationError;
    }
    ```

* Implementation (`com.{YOUR_PROJECT}.cif.core.models.MyProductImpl`)
    ```java
    package com.venia.cif.core.models;

    import com.adobe.cq.commerce.core.components.models.product.Asset;
    import com.adobe.cq.commerce.core.components.models.product.Product;
    import com.adobe.cq.commerce.core.components.models.product.Variant;
    import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
    import com.adobe.cq.commerce.core.components.models.retriever.ProductRetriever;
    import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
    import com.shopify.graphql.support.SchemaViolationError;
    import org.apache.sling.api.SlingHttpServletRequest;
    import org.apache.sling.models.annotations.Model;
    import org.apache.sling.models.annotations.Via;
    import org.apache.sling.models.annotations.injectorspecific.Self;
    import org.apache.sling.models.annotations.via.ResourceSuperType;

    import javax.annotation.PostConstruct;
    import java.util.List;

    @Model(adaptables = SlingHttpServletRequest.class, adapters = MyProduct.class, resourceType = MyProductImpl.RESOURCE_TYPE)
    public class MyProductImpl implements MyProduct {

        protected static final String RESOURCE_TYPE = "venia/components/commerce/myproduct";

        @Self @Via(type = ResourceSuperType.class)
        private Product product;

        private ProductRetriever productRetriever;

        @PostConstruct
        public void initModel() {
            productRetriever = product.getProductRetriever();

            // Pass your custom partial query to the ProductRetriever. This class will automatically take care of executing your query as soon
            // as you try to access any product property.
            productRetriever.extendProductQueryWith(p ->
                p.createdAt()
                .addCustomSimpleField("is_returnable"));
        }

        @Override public String getCreatedAt() {
            return productRetriever.fetchProduct().getCreatedAt();
        }

        @Override public String isReturnable() throws SchemaViolationError {
            return productRetriever.fetchProduct().getAsString("is_returnable");
        }

        @Override public Boolean getFound() {
            return product.getFound();
        }

        /* ... Additional getters from interface ... */
    }
    ```

#### Proxy Component
* Proxy Component (`apps/{YOUR_PROJECT}/components/commerce/myproduct/.content.xml`)
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
        jcr:primaryType="cq:Component"
        jcr:title="Not A Product"
        jcr:description="Not a product component"
        sling:resourceSuperType="core/cif/components/commerce/product/v1/product"
        componentGroup="venia"/>
    ```

* Template Overlay (`apps/{YOUR_PROJECT}/components/commerce/myproduct/myproduct.html`)
    ```html
    <sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html"
        data-sly-use.variantsTpl="variantselector.html"
        data-sly-use.galleryTpl="gallery.html"
        data-sly-use.priceTpl="price.html"
        data-sly-use.actionsTpl="actions.html"
        data-sly-use.quantityTpl="quantity.html"
        data-sly-use.product="com.venia.cif.core.models.MyProduct"
        data-sly-use.page="com.adobe.cq.wcm.core.components.models.Page">

        <sly data-sly-call="${clientlib.all @ categories='core.cif.components.product.v1'}"/>
        <form class="productFullDetail__root"
            data-configurable="${product.configurable}"
            data-cmp-is="product"
            data-locale="${page.language}"
            data-load-client-price="${product.loadClientPrice}">
            <sly data-sly-test.found="${product.found}">
                <section class="productFullDetail__title">
                    <h1 class="productFullDetail__productName">
                        <span role="name">${product.name}</span>
                        <span class="productFullDetail__createdAt">${product.createdAt}</span> <!-- Use the new property here -->
                    </h1>
                    <sly data-sly-call="${priceTpl.price @ product=product}"></sly>
                </section>
                <section class="productFullDetail__imageCarousel">
                    <sly data-sly-call="${galleryTpl.gallery @ product=product}" data-sly-unwrap></sly>
                </section>
                <section class="productFullDetail__options" data-sly-test="${product.configurable}" data-variants="${product.variantsJson}">
                    <sly data-sly-call="${variantsTpl.variants @ product=product}" data-sly-unwrap></sly>
                </section>
                <section class="productFullDetail__quantity productFullDetail__section">
                    <sly data-sly-call="${quantityTpl.quantity @ product=product}"></sly>
                </section>
                <section class="productFullDetail__cartActions productFullDetail__section">
                    <sly data-sly-call="${actionsTpl.actions @ product=product}"></sly>
                </section>
                <section class="productFullDetail__description productFullDetail__section">
                    <h2 class="productFullDetail__descriptionTitle productFullDetail__sectionTitle">
                        Product Description
                    </h2>
                    <div class="richText__root" role="description">
                        ${product.description @ context='html'}
                    </div>
                </section>
                <section class="productFullDetail__details productFullDetail__section">
                    <h2 class="productFullDetail__detailsTitle productFullDetail__sectionTitle">SKU</h2>
                    <strong role="sku">${product.sku}</strong>
                </section>
            </sly>
            <p data-sly-test="${!found}">Product not found.</p>
        </form>
    </sly>
    ```



