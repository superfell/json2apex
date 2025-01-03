# JSON2Apex

This app allows a user to paste in an instance of a json document, and have it generate strongly typed Apex code that can deserialize it.

Its written in Java using the Play! framework.

For many years it was available at https://json2apex.herokuapp.com however Heroku discontinued the free tier and its no longer online there.

You can now access it instead at https://superfell.com/json2apex

Or you can run it locally via docker with

`docker run  -p 9091:9091 -d  ghcr.io/superfell/json2apex:latest`

once running open your browser at http://localhost:9091

JSON2Apex is open source under the MIT license.

## About integration-tests

The integration-test folder contains a modified version of beatbox that can call the compileClass API in the Apex API, there's a test
runner that will post the test json's to a running instance of the app and then send them to saleforce, if salesforce reports a compilation
error, that's logged in the test runner output.

the test runner requires python3 and a salesforce developer edition org.

_Warning, it'll overwrite any class called JSON2ApexIntegration[_Test] you might have_

run the integration tests from the integration-test folder with

    python3 tests.py <sfdc_username> <sfdc_password>

you'll get output similar to

    Logged in at https://na45.salesforce.com/services/Soap/u/42.0/00D300000000QSf
    Compiling 2 scripts generated from basic_object.json                explicitParse:False ✔ success
    Compiling 2 scripts generated from basic_object.json                explicitParse:True  ✔ success
    Compiling 2 scripts generated from dot_in_fieldname.json            explicitParse:False ✔ success
    Compiling 2 scripts generated from dot_in_fieldname.json            explicitParse:True  ✔ success
    Compiling 2 scripts generated from dot_in_objectname.json           explicitParse:False ✔ success
    Compiling 2 scripts generated from dot_in_objectname.json           explicitParse:True  ✔ success
    Compiling 2 scripts generated from field_array.json                 explicitParse:False ✔ success
    Compiling 2 scripts generated from field_array.json                 explicitParse:True  ✔ success
    Compiling 2 scripts generated from underscore_in_fieldname.json     explicitParse:False ✔ success
    Compiling 2 scripts generated from underscore_in_fieldname.json     explicitParse:True  ✔ success

Note that beatbox & xmltramp is GPL, not MIT like the rest of this project.
