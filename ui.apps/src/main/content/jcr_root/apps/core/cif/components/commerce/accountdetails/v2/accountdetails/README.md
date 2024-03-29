# Account Details (v2)

The Account Details is a React component providing CRUD operations on the account details of a signed in shopper.

## Features:

- Display the full name and the e-mail of the shopper
- Display just a generic message if the user is anonymous
- Allow a shopper to change the first name, last name, email address and password 

## Implementation

This AEM component only renders a `div` element acting as a container for the [AccountDetails](/react-components/src/components/AccountDetails) React component.

## BEM Description

```
BLOCK accountdetails
    ELEMENT accountdetails__root
    ELEMENT accountdetails__body    
```

## License information

-   Vendor: Adobe
-   Version: v2
-   Compatibility: AEM as a Cloud Service / 6.5
-   Status: production-ready
