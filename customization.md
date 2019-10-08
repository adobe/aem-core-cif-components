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

    Use a custom GraphQL schema and create a custom query to implement and extend the component.

    *Example: Include a custom inventory system in your schema to display additional inventory information that are not present in the Magento schema.*

## Customization Layers
1. Sightly Templates
1. Sling Model Interface
1. Sling Model Implementation
1. Data Layer (GraphQL Query)
1. GraphQL Schema (Model Classes)

## Use Case to Layer Mapping

| Use Case | Template | Interface | Implementation | Data Layer | Schema |
| -------- | -------- | --------- | -------------- | ---------- | ------ |
| A        | ✔️       | ✔️        | ❌            | ✔️          | ✔️    |
| B        | ❌       | ❌        | ❌            | ❌          | ✔️    |
| C        | ❌       | ❌        | ❌            | ❌          | ❌    |

✔️ no adaption needed, ❌adaption needed

## Examples

### Use Case A
Please refer to [Delegation Pattern for Sling Models](https://github.com/adobe/aem-core-wcm-components/wiki/Delegation-Pattern-for-Sling-Models).

### Use Case B

#### Custom Sling Model
* Interface (`com.{YOUR_PROJECT}.cif.core.models.NotAProduct`)
    ```java
    package com.venia.cif.core.models;

    import com.adobe.cq.commerce.core.components.models.product.Product;
    import org.osgi.annotation.versioning.ProviderType;

    @ProviderType
    public interface NotAProduct extends Product {
        // Extend the existing interface with the additional properties which you 
        // want to expose to the HTL template.
        public String getCreatedAt();
    }
    ```

* Implementation (`com.{YOUR_PROJECT}.cif.core.models.NotAProductImpl`)
    ```java
    package com.venia.cif.core.models;

    import com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl;
    import com.adobe.cq.commerce.core.components.models.product.Asset;
    import com.adobe.cq.commerce.core.components.models.product.Variant;
    import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
    import com.adobe.cq.commerce.magento.graphql.FilterTypeInput;
    import com.adobe.cq.commerce.magento.graphql.Operations;
    import com.adobe.cq.commerce.magento.graphql.ProductFilterInput;
    import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
    import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
    import com.adobe.cq.commerce.magento.graphql.QueryQuery;
    import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;
    import org.apache.sling.api.SlingHttpServletRequest;
    import org.apache.sling.models.annotations.Model;
    import org.apache.sling.models.annotations.Via;
    import org.apache.sling.models.annotations.injectorspecific.Self;
    import org.apache.sling.models.annotations.via.ResourceSuperType;

    import javax.annotation.PostConstruct;
    import java.util.List;

    @Model(adaptables = SlingHttpServletRequest.class, adapters = NotAProduct.class, resourceType = NotAProductImpl.RESOURCE_TYPE)
    public class NotAProductImpl implements NotAProduct {

        protected static final String RESOURCE_TYPE = "venia/components/commerce/notaproduct";

        @Self @Via(type = ResourceSuperType.class)
        private ProductImpl product;

        @PostConstruct
        private void initModel() {
            String slug = product.parseProductSlug();

            // Pass your custom query to the ProductRetriever. This class will
            // automatically take care of executing your query as soon as you
            // try to access a product property.
            product.productRetriever.setQuery(generateQuery(slug));
        }

        // Create your own  GraphQL query and feel free to re-use as many of the
        // partial queries exposed by the parent class as you wish.
        public String generateQuery(String slug) {
            FilterTypeInput input = new FilterTypeInput().setEq(slug);
            ProductFilterInput filter = new ProductFilterInput().setUrlKey(input);
            QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

            // GraphQL query
            ProductsQueryDefinition queryArgs = q -> q.items(generateProductQuery());
            return Operations.query(query -> query
                    .products(searchArgs, queryArgs)
                    .storeConfig(product.generateStoreConfigQuery())).toString();
        }

        @Override public Boolean getFound() {
            return product.getFound();
        }

        // ... provide getters to fully implement the interface

        // Getter for newly exposed product attribute
        @Override public String getCreatedAt() {
            return product.productRetriever.getProduct().getCreatedAt();
        }

        /* --- Custom GraphQL queries --- */

        public ProductInterfaceQueryDefinition generateProductQuery() {
            return q -> q
                .sku()
                .name()
                .description(d -> d.html())
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .createdAt() // New property
                .stockStatus()
                .price(product.generatePriceQuery())
                .mediaGalleryEntries(g -> g
                    .disabled()
                    .file()
                    .label()
                    .position()
                    .mediaType())
                .onConfigurableProduct(cp -> cp
                    .configurableOptions(o -> o
                        .label()
                        .attributeCode()
                        .values(v -> v
                            .valueIndex()
                            .label()))
                    .variants(v -> v
                        .attributes(a -> a
                            .code()
                            .valueIndex())
                        .product(generateSimpleProductQuery())));
        }

        public SimpleProductQueryDefinition generateSimpleProductQuery() {
            return q -> q
                .sku()
                .name()
                .description(d -> d.html())
                .image(i -> i.label().url())
                .thumbnail(t -> t.label().url())
                .urlKey()
                .createdAt() // New property
                .stockStatus()
                .color()
                .price(product.generatePriceQuery())
                .mediaGalleryEntries(g -> g
                    .disabled()
                    .file()
                    .label()
                    .position()
                    .mediaType());
        }
    }
    ```

#### Proxy Component
* Proxy Component (`apps/{YOUR_PROJECT}/components/commerce/notaproduct/.content.xml`)
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
        jcr:primaryType="cq:Component"
        jcr:title="Not A Product"
        jcr:description="Not a product component"
        sling:resourceSuperType="core/cif/components/commerce/product/v1/product"
        componentGroup="venia"/>
    ```

* Template Overlay (`apps/{YOUR_PROJECT}/components/commerce/notaproduct/notaproduct.html`)
    ```html
    <sly data-sly-use.clientlib="/libs/granite/sightly/templates/clientlib.html"
        data-sly-use.variantsTpl="variantselector.html"
        data-sly-use.galleryTpl="gallery.html"
        data-sly-use.priceTpl="price.html"
        data-sly-use.actionsTpl="actions.html"
        data-sly-use.quantityTpl="quantity.html"
        data-sly-use.product="com.venia.cif.core.models.NotAProduct"
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

### Use Case C

## TODOs

### Use Case A
* Should work out of the box.

### Use Case B
* Decouple data retrieval from sling model.
* Remove data retrieval from the sling model's `initModel` method. (e.g. provide a wrapper that executes query only when the first getter is called).
* Keep using `MagentoGraphqlClient`
* Allows using of CIF core components package with customizations in proxy components

### Use Case C
* Make data retrival layer compatible with different models. (e.g. allow result to be deserialized into multiple models)
* Use generic `GraphqlClient`
* Newly generated schema replaces existing schema, then CIF core components package needs to be rebuilt



