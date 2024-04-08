# Security matter 
## Secret key generation
>* A secret key is needed in order to authenticate and get a signed JWT Token
>* Java keytool can generate strong keys inside a file already known as the keystore
>* The keystore is not transportable, in theory you can't use a keystore from another
>* You must generate your own consistent with what is configured in application.properties

`cd src/main/resources/security`

Then

    keytool -genseckey -keystore .keys -storetype jceks -storepass storepop -keyalg HMacSHA256 -keysize 2048 -alias HS256 -keypass oungawa

Enjoy ...

## Trust a web application through its X.509 Certificate
>* When communicate with an external https that send request, we must acceppt its certificate
>* Java keytool can store trust certificate in a commonly called "truststore"
>* First we need to obtain the certificate : 

    Go to chrome and https://identification-pprd.agriculture.gouv.fr/eap/login
    Then open developers tools F12 and go to Security Tab
    In Security Overview clic "View certificate"
    In the popup (Details tab) select "Display all" and click on "Copy to file" 
    Select "next" and check X.509 binary encoded DER (.cer)
    Then export into pprod-maaf-cas.cer for instance
    You got it ! Nice !
>*  Now we can register the certificate in the default Java truststore
>*  Go to your JDK in jre/lib/security and see the file named "cacert" (this is the certificate truststore)
>*  Copy the certificate into the directory and lauch this keytool command from the jre/lib/security:

    keytool -importcert -trustcacerts -file pprod-maaf-cas.cer -alias identification-pprd.agriculture.gouv.fr -keystore cacerts
    
Great ...