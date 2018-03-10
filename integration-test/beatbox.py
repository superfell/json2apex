"""beatbox: Makes the salesforce.com SOAP API easily accessible."""
from __future__ import print_function

from beatbox_six import PY3, BytesIO, http_client, text_type, urlparse, xrange
import gzip
import datetime
import xmltramp
from xmltramp import islst
from xml.sax.saxutils import XMLGenerator
from xml.sax.saxutils import quoteattr
from xml.sax.xmlreader import AttributesNSImpl

__version__ = "0.96"
__author__ = "Simon Fell"
__credits__ = "Mad shouts to the sforce possie"
__copyright__ = "(C) 2006-2015 Simon Fell. GNU GPL 2."

# global constants for namespace strings, used during serialization
_apexNs = "http://soap.sforce.com/2006/08/apex"
_partnerNs = "urn:partner.soap.sforce.com"
_sobjectNs = "urn:sobject.partner.soap.sforce.com"
_envNs = "http://schemas.xmlsoap.org/soap/envelope/"
_noAttrs = AttributesNSImpl({}, {})

# global constants for xmltramp namespaces, used to access response data
_tPartnerNS = xmltramp.Namespace(_partnerNs)
_tSObjectNS = xmltramp.Namespace(_sobjectNs)
_tSoapNS = xmltramp.Namespace(_envNs)
_tApexNS = xmltramp.Namespace(_apexNs)

# global config
gzipRequest = True    # are we going to gzip the request ?
gzipResponse = True   # are we going to tell the server to gzip the response ?
forceHttp = False     # force all connections to be HTTP, for debugging


def makeConnection(scheme, host, timeout=1200):
    kwargs = {'timeout': timeout}
    if forceHttp or scheme.upper() == 'HTTP':
        return http_client.HTTPConnection(host, **kwargs)
    return http_client.HTTPSConnection(host, **kwargs)


# the main sforce client proxy class
class Client(object):
    def __init__(self):
        self.batchSize = 500
        self.serverUrl = "https://login.salesforce.com/services/Soap/u/36.0"
        self.__conn = None
        self.timeout = 15

    def __del__(self):
        if self.__conn:
            self.__conn.close()

    # login, the serverUrl and sessionId are automatically handled, returns the loginResult structure
    def login(self, username, password):
        lr = LoginRequest(self.serverUrl, username, password).post()
        self.useSession(str(lr[_tPartnerNS.sessionId]), str(lr[_tPartnerNS.serverUrl]))
        return lr

    # perform a portal login, orgId is always needed, portalId is needed for new style portals
    # is not required for the old self service portal
    # for the self service portal, only the login request will work, self service users don't
    # get API access, for new portals, the users should have API acesss, and can call the rest
    # of the API.
    def portalLogin(self, username, password, orgId, portalId):
        lr = PortalLoginRequest(self.serverUrl, username, password, orgId, portalId).post()
        self.useSession(str(lr[_tPartnerNS.sessionId]), str(lr[_tPartnerNS.serverUrl]))
        return lr

    # initialize from an existing sessionId & serverUrl, useful if we're being launched via a custom link
    def useSession(self, sessionId, serverUrl):
        self.sessionId = sessionId
        self.__serverUrl = serverUrl
        (scheme, host, path, params, query, frag) = urlparse(self.__serverUrl)
        self.__conn = makeConnection(scheme, host)

    # calls logout which invalidates the current sessionId, in general its better to not call this and just
    # let the sessions expire on their own.
    def logout(self):
        return LogoutRequest(self.__serverUrl, self.sessionId).post(self.__conn, True)

    def compileApex(self, scripts):
        return CompileClassRequest(self.__serverUrl.replace("/Soap/u/", "/Soap/s/"), self.sessionId, scripts).post()
		
    # set the batchSize property on the Client instance to change the batchsize for query/queryMore
    def query(self, soql):
        return QueryRequest(self.__serverUrl, self.sessionId, self.batchSize, soql).post(self.__conn)

    # query include deleted and archived rows.
    def queryAll(self, soql):
        return QueryRequest(self.__serverUrl, self.sessionId, self.batchSize, soql, "queryAll").post(self.__conn)

    def queryMore(self, queryLocator):
        return QueryMoreRequest(self.__serverUrl, self.sessionId, self.batchSize, queryLocator).post(self.__conn)

    def search(self, sosl):
        return SearchRequest(self.__serverUrl, self.sessionId, sosl).post(self.__conn)

    def getUpdated(self, sObjectType, start, end):
        return GetUpdatedRequest(self.__serverUrl, self.sessionId, sObjectType, start, end).post(self.__conn)

    def getDeleted(self, sObjectType, start, end):
        return GetDeletedRequest(self.__serverUrl, self.sessionId, sObjectType, start, end).post(self.__conn)

    def retrieve(self, fields, sObjectType, ids):
        return RetrieveRequest(self.__serverUrl, self.sessionId, fields, sObjectType, ids).post(self.__conn)

    # sObjects can be 1 or a list, returns a single save result or a list
    def create(self, sObjects):
        return CreateRequest(self.__serverUrl, self.sessionId, sObjects).post(self.__conn)

    # sObjects can be 1 or a list, returns a single save result or a list
    def update(self, sObjects):
        return UpdateRequest(self.__serverUrl, self.sessionId, sObjects).post(self.__conn)

    # sObjects can be 1 or a list, returns a single upsert result or a list
    def upsert(self, externalIdName, sObjects):
        return UpsertRequest(self.__serverUrl, self.sessionId, externalIdName, sObjects).post(self.__conn)

    # ids can be 1 or a list, returns a single delete result or a list
    def delete(self, ids):
        return DeleteRequest(self.__serverUrl, self.sessionId, ids).post(self.__conn)

    # ids can be 1 or a list, returns a single delete result or a list
    def undelete(self, ids):
        return UndeleteRequest(self.__serverUrl, self.sessionId, ids).post(self.__conn)

    # leadConverts can be 1 or a list of dictionaries, each dictionary should be filled out as per
    # the LeadConvert type in the WSDL.
    #   <element name="accountId"              type="tns:ID" nillable="true"/>
    #   <element name="contactId"              type="tns:ID" nillable="true"/>
    #   <element name="convertedStatus"        type="xsd:string"/>
    #   <element name="doNotCreateOpportunity" type="xsd:boolean"/>
    #   <element name="leadId"                 type="tns:ID"/>
    #   <element name="opportunityName"        type="xsd:string" nillable="true"/>
    #   <element name="overwriteLeadSource"    type="xsd:boolean"/>
    #   <element name="ownerId"                type="tns:ID"     nillable="true"/>
    #   <element name="sendNotificationEmail"  type="xsd:boolean"/>
    def convertLead(self, leadConverts):
        return ConvertLeadRequest(self.__serverUrl, self.sessionId, leadConverts).post(self.__conn)

    # sObjectTypes can be 1 or a list, returns a single describe result or a list of them
    def describeSObjects(self, sObjectTypes):
        return DescribeSObjectsRequest(self.__serverUrl, self.sessionId, sObjectTypes).post(self.__conn)

    def describeGlobal(self):
        return AuthenticatedRequest(self.__serverUrl, self.sessionId, "describeGlobal").post(self.__conn)

    def describeLayout(self, sObjectType):
        return DescribeLayoutRequest(self.__serverUrl, self.sessionId, sObjectType).post(self.__conn)

    def describeTabs(self):
        return AuthenticatedRequest(self.__serverUrl, self.sessionId, "describeTabs").post(self.__conn, True)

    def describeSearchScopeOrder(self):
        return AuthenticatedRequest(self.__serverUrl, self.sessionId, "describeSearchScopeOrder"
                                    ).post(self.__conn, True)

    def describeQuickActions(self, actions):
        return DescribeQuickActionsRequest(self.__serverUrl, self.sessionId, actions).post(self.__conn, True)

    def describeAvailableQuickActions(self, parentType=None):
        return DescribeAvailableQuickActionsRequest(self.__serverUrl, self.sessionId, parentType
                                                    ).post(self.__conn, True)

    def performQuickActions(self, actions):
        return PerformQuickActionsRequest(self.__serverUrl, self.sessionId, actions).post(self.__conn, True)

    def getServerTimestamp(self):
        return str(AuthenticatedRequest(self.__serverUrl, self.sessionId, "getServerTimestamp"
                                        ).post(self.__conn)[_tPartnerNS.timestamp])

    def resetPassword(self, userId):
        return ResetPasswordRequest(self.__serverUrl, self.sessionId, userId).post(self.__conn)

    def setPassword(self, userId, password):
        SetPasswordRequest(self.__serverUrl, self.sessionId, userId, password).post(self.__conn)

    def getUserInfo(self):
        return AuthenticatedRequest(self.__serverUrl, self.sessionId, "getUserInfo").post(self.__conn)

    # def convertLead(self, convertLeads):


class IterClient(Client):

    def __init__(self):
        super(IterClient, self).__init__()

    def gatherRecords(self, queryHandle):
        while 1:
            for elem in queryHandle[_tPartnerNS.records:]:
                yield elem
            if str(queryHandle[_tPartnerNS.done]) == 'true':
                break
            else:
                queryHandle = self.queryMore(queryHandle.queryLocator)

    def chunkRequests(self, collection, chunkLength=None):
        if not islst(collection):
            yield [collection]
        else:
            if chunkLength is None:
                chunkLength = self.batchSize
            for i in xrange(0, len(collection), chunkLength):
                yield collection[i:i + chunkLength]

    def query(self, soql):
        return self.gatherRecords(super(IterClient, self).query(soql))

    def queryAll(self, soql):
        return self.gatherRecords(super(IterClient, self).queryAll(soql))

    def create(self, sObjects, chunkLength=None):
        for chunk in self.chunkRequests(sObjects, chunkLength=chunkLength):
            if len(chunk) == 1:
                responses = [super(IterClient, self).create(chunk)]
            else:
                responses = super(IterClient, self).create(chunk)
            for response in responses:
                yield response

    def update(self, sObjects, chunkLength=None):
        for chunk in self.chunkRequests(sObjects, chunkLength=chunkLength):
            if len(chunk) == 1:
                responses = [super(IterClient, self).update(chunk)]
            else:
                responses = super(IterClient, self).update(chunk)
            for response in responses:
                yield response

    def upsert(self, externalIdName, sObjects, chunkLength=None):
        for chunk in self.chunkRequests(sObjects, chunkLength=chunkLength):
            if len(chunk) == 1:
                responses = [super(IterClient, self).upsert(externalIdName, chunk)]
            else:
                responses = super(IterClient, self).upsert(externalIdName, chunk)

            for response in responses:
                yield response

    def delete(self, ids, chunkLength=None):
        for chunk in self.chunkRequests(ids, chunkLength=chunkLength):
            if len(chunk) == 1:
                responses = [super(IterClient, self).delete(chunk)]
            else:
                responses = super(IterClient, self).delete(chunk)
            for response in responses:
                yield response

    def undelete(self, ids, chunkLength=None):
        for chunk in self.chunkRequests(ids, chunkLength=chunkLength):
            if len(chunk) == 1:
                responses = [super(IterClient, self).undelete(chunk)]
            else:
                responses = super(IterClient, self).undelete(chunk)
            for response in responses:
                yield response


# fixed version of XmlGenerator, handles unqualified attributes correctly
class BeatBoxXmlGenerator(XMLGenerator):
    def __init__(self, destination, encoding):
        self._out = destination
        XMLGenerator.__init__(self, destination, encoding)

    def makeName(self, name):
        if name[0] is None:
            # if the name was not namespace-scoped, use the qualified part
            return name[1]
        # else try to restore the original prefix from the namespace
        return self._current_context[name[0]] + ":" + name[1]

    def startElementNS(self, name, qname, attrs):
        self._write(text_type('<' + self.makeName(name)))

        for pair in self._undeclared_ns_maps:
            self._write(text_type(' xmlns:%s="%s"' % pair))
        self._undeclared_ns_maps = []

        for (name, value) in attrs.items():
            self._write(text_type(' %s=%s' % (self.makeName(name), quoteattr(value))))
        self._write(text_type('>'))


# general purpose xml writer, does a bunch of useful stuff above & beyond XmlGenerator
class XmlWriter(object):
    def __init__(self, doGzip):
        self.__buf = BytesIO()
        if doGzip:
            self.__gzip = gzip.GzipFile(mode='wb', fileobj=self.__buf)
            stm = self.__gzip
        else:
            stm = self.__buf
            self.__gzip = None
        self.xg = BeatBoxXmlGenerator(stm, "utf-8")
        self.xg.startDocument()
        self.__elems = []

    def startPrefixMapping(self, prefix, namespace):
        self.xg.startPrefixMapping(prefix, namespace)

    def endPrefixMapping(self, prefix):
        self.xg.endPrefixMapping(prefix)

    def startElement(self, namespace, name, attrs=_noAttrs):
        self.xg.startElementNS((namespace, name), name, attrs)
        self.__elems.append((namespace, name))

    # if value is a list, then it writes out repeating elements, one for each value
    def writeStringElement(self, namespace, name, value, attrs=_noAttrs):
        if islst(value):
            for v in value:
                self.writeStringElement(namespace, name, v, attrs)
        else:
            self.startElement(namespace, name, attrs)
            self.characters(value)
            self.endElement()

    def endElement(self):
        e = self.__elems[-1]
        self.xg.endElementNS(e, e[1])
        del self.__elems[-1]

    def characters(self, s):
        # todo base64 ?
        if isinstance(s, datetime.datetime):
            # todo, timezones
            s = s.isoformat()
        elif isinstance(s, datetime.date):
            # todo, try isoformat
            s = "%04d-%02d-%02d" % (s.year, s.month, s.day)
        elif isinstance(s, int):
            s = str(s)
        elif isinstance(s, float):
            s = str(s)
        self.xg.characters(s)

    def endDocument(self):
        self.xg.endDocument()
        if (self.__gzip is not None):
            self.__gzip.close()
        return self.__buf.getvalue()


# exception class for soap faults
class SoapFaultError(Exception):
    def __init__(self, faultCode, faultString):
        self.faultCode = faultCode
        self.faultString = faultString

    def __str__(self):
        return repr(self.faultCode) + " " + repr(self.faultString)


# soap specific stuff ontop of XmlWriter
class SoapWriter(XmlWriter):
    __xsiNs = "http://www.w3.org/2001/XMLSchema-instance"

    def __init__(self):
        XmlWriter.__init__(self, gzipRequest)
        self.startPrefixMapping("s", _envNs)
        self.startPrefixMapping("p", _partnerNs)
        self.startPrefixMapping("o", _sobjectNs)
        self.startPrefixMapping("x", SoapWriter.__xsiNs)
        self.startPrefixMapping("a", _apexNs)
        self.startElement(_envNs, "Envelope")

    def writeStringElement(self, namespace, name, value, attrs=_noAttrs):
        if value is None:
            if attrs:
                attrs[(SoapWriter.__xsiNs, "nil")] = 'true'
            else:
                attrs = {(SoapWriter.__xsiNs, "nil"): 'true'}
            value = ""
        XmlWriter.writeStringElement(self, namespace, name, value, attrs)

    def endDocument(self):
        self.endElement()  # envelope
        self.endPrefixMapping("o")
        self.endPrefixMapping("p")
        self.endPrefixMapping("s")
        self.endPrefixMapping("x")
        return XmlWriter.endDocument(self)


# processing for a single soap request / response
class SoapEnvelope(object):
    def __init__(self, serverUrl, operationName, serviceNs=_partnerNs, clientId="BeatBox/" + __version__):
        self.serverUrl = serverUrl
        self.operationName = operationName
        self.clientId = clientId
        self.serviceNs = serviceNs

    def writeHeaders(self, writer):
        pass

    def writeBody(self, writer):
        pass

    def makeEnvelope(self):
        s = SoapWriter()
        s.startElement(_envNs, "Header")
        s.characters("\n")
        s.startElement(self.serviceNs, "CallOptions")
        s.writeStringElement(self.serviceNs, "client", self.clientId)
        s.endElement()
        s.characters("\n")
        self.writeHeaders(s)
        s.endElement()  # Header
        s.startElement(_envNs, "Body")
        s.characters("\n")
        s.startElement(self.serviceNs, self.operationName)
        self.writeBody(s)
        s.endElement()  # operation
        s.endElement()  # body
        return s.endDocument()

    # does all the grunt work,
    #   serializes the request,
    #   makes a http request,
    #   passes the response to tramp
    #   checks for soap fault
    #   todo: check for mU='1' headers
    #   returns the relevant result from the body child
    def post(self, conn=None, alwaysReturnList=False):
        headers = {"User-Agent": "BeatBox/" + __version__,
                   "SOAPAction": "\"\"",
                   "Content-Type": "text/xml; charset=utf-8"}
        if gzipResponse:
            headers['accept-encoding'] = 'gzip'
        if gzipRequest:
            headers['content-encoding'] = 'gzip'
        close = False
        (scheme, host, path, params, query, frag) = urlparse(self.serverUrl)
        if conn is None:
            conn = makeConnection(scheme, host)
            close = True
        rawRequest = self.makeEnvelope()
        # print(rawRequest)
        conn.request("POST", path, rawRequest, headers)
        response = conn.getresponse()
        rawResponse = response.read()
        if response.getheader('content-encoding', '') == 'gzip':
            rawResponse = gzip.GzipFile(fileobj=BytesIO(rawResponse)).read()
        if close:
            conn.close()
        string_response = rawResponse.decode('utf-8') if PY3 else rawResponse
        tramp = xmltramp.parse(string_response)
        try:
            faultString = str(tramp[_tSoapNS.Body][_tSoapNS.Fault].faultstring)
            faultCode = str(tramp[_tSoapNS.Body][_tSoapNS.Fault].faultcode).split(':')[-1]
            raise SoapFaultError(faultCode, faultString)
        except KeyError:
            pass
        # first child of body is XXXXResponse
        result = tramp[_tSoapNS.Body][0]
        # it contains either a single child, or for a batch call multiple children
        if alwaysReturnList or len(result) > 1:
            return result[:]
        else:
            return result[0]


class LoginRequest(SoapEnvelope):
    def __init__(self, serverUrl, username, password):
        SoapEnvelope.__init__(self, serverUrl, "login")
        self.__username = username
        self.__password = password

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "username", self.__username)
        s.writeStringElement(_partnerNs, "password", self.__password)


class PortalLoginRequest(LoginRequest):
    def __init__(self, serverUrl, username, password, orgId, portalId):
        LoginRequest.__init__(self, serverUrl, username, password)
        self.__orgId = orgId
        self.__portalId = portalId

    def writeHeaders(self, s):
        s.startElement(_partnerNs, "LoginScopeHeader")
        s.writeStringElement(_partnerNs, "organizationId", self.__orgId)
        if (not (self.__portalId is None or self.__portalId == "")):
            s.writeStringElement(_partnerNs, "portalId", self.__portalId)
        s.endElement()


# base class for all methods that require a sessionId
class AuthenticatedRequest(SoapEnvelope):
    def __init__(self, serverUrl, sessionId, operationName):
        SoapEnvelope.__init__(self, serverUrl, operationName, _apexNs)
        self.sessionId = sessionId

    def writeHeaders(self, s):
        s.startElement(self.serviceNs, "SessionHeader")
        s.writeStringElement(self.serviceNs, "sessionId", self.sessionId)
        s.endElement()

    def writeDict(self, s, elemName, d):
        if islst(d):
            for o in d:
                self.writeDict(s, elemName, o)
        else:
            s.startElement(self.serviceNs, elemName)
            for fn in d.keys():
                if (isinstance(d[fn], dict)):
                    self.writeDict(s, d[fn], fn)
                else:
                    s.writeStringElement(_sobjectNs, fn, d[fn])
            s.endElement()

    def writeSObjects(self, s, sObjects, elemName="sObjects"):
        if islst(sObjects):
            for o in sObjects:
                self.writeSObjects(s, o, elemName)
        else:
            s.startElement(_partnerNs, elemName)
            # type has to go first
            s.writeStringElement(_sobjectNs, "type", sObjects['type'])
            for fn in sObjects.keys():
                if (fn != 'type'):
                    if (isinstance(sObjects[fn], dict)):
                        self.writeSObjects(s, sObjects[fn], fn)
                    else:
                        s.writeStringElement(_sobjectNs, fn, sObjects[fn])
            s.endElement()


class LogoutRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "logout")


class QueryOptionsRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, batchSize, operationName):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, operationName)
        self.batchSize = batchSize

    def writeHeaders(self, s):
        AuthenticatedRequest.writeHeaders(self, s)
        s.startElement(_partnerNs, "QueryOptions")
        s.writeStringElement(_partnerNs, "batchSize", self.batchSize)
        s.endElement()


class QueryRequest(QueryOptionsRequest):
    def __init__(self, serverUrl, sessionId, batchSize, soql, operationName="query"):
        QueryOptionsRequest.__init__(self, serverUrl, sessionId, batchSize, operationName)
        self.__query = soql

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "queryString", self.__query)


class QueryMoreRequest(QueryOptionsRequest):
    def __init__(self, serverUrl, sessionId, batchSize, queryLocator):
        QueryOptionsRequest.__init__(self, serverUrl, sessionId, batchSize, "queryMore")
        self.__queryLocator = queryLocator

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "queryLocator", self.__queryLocator)


class SearchRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, sosl):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "search")
        self.__query = sosl

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "searchString", self.__query)


class GetUpdatedRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, sObjectType, start, end, operationName="getUpdated"):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, operationName)
        self.__sObjectType = sObjectType
        self.__start = start
        self.__end = end

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "sObjectType", self.__sObjectType)
        s.writeStringElement(_partnerNs, "startDate", self.__start)
        s.writeStringElement(_partnerNs, "endDate", self.__end)


class GetDeletedRequest(GetUpdatedRequest):
    def __init__(self, serverUrl, sessionId, sObjectType, start, end):
        GetUpdatedRequest.__init__(self, serverUrl, sessionId, sObjectType, start, end, "getDeleted")


class UpsertRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, externalIdName, sObjects):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "upsert")
        self.__externalIdName = externalIdName
        self.__sObjects = sObjects

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "externalIDFieldName", self.__externalIdName)
        self.writeSObjects(s, self.__sObjects)


class UpdateRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, sObjects, operationName="update"):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, operationName)
        self.__sObjects = sObjects

    def writeBody(self, s):
        self.writeSObjects(s, self.__sObjects)


class CreateRequest(UpdateRequest):
    def __init__(self, serverUrl, sessionId, sObjects):
        UpdateRequest.__init__(self, serverUrl, sessionId, sObjects, "create")


class DeleteRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, ids, operationName="delete"):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, operationName)
        self.__ids = ids

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "id", self.__ids)


class UndeleteRequest(DeleteRequest):
    def __init__(self, serverUrl, sessionId, ids):
        DeleteRequest.__init__(self, serverUrl, sessionId, ids, "undelete")


class RetrieveRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, fields, sObjectType, ids):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "retrieve")
        self.__fields = fields
        self.__sObjectType = sObjectType
        self.__ids = ids

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "fieldList", self.__fields)
        s.writeStringElement(_partnerNs, "sObjectType", self.__sObjectType)
        s.writeStringElement(_partnerNs, "ids", self.__ids)


class ResetPasswordRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, userId):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "resetPassword")
        self.__userId = userId

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "userId", self.__userId)


class SetPasswordRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, userId, password):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "setPassword")
        self.__userId = userId
        self.__password = password

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "userId", self.__userId)
        s.writeStringElement(_partnerNs, "password", self.__password)


class ConvertLeadRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, leadConverts):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "convertLead")
        self.__leads = leadConverts

    def writeBody(self, s):
        self.writeDict(s, "leadConverts", self.__leads)


class DescribeSObjectsRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, sObjectTypes):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "describeSObjects")
        self.__sObjectTypes = sObjectTypes

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "sObjectType", self.__sObjectTypes)


class DescribeLayoutRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, sObjectType):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "describeLayout")
        self.__sObjectType = sObjectType

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "sObjectType", self.__sObjectType)


class DescribeQuickActionsRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, actions):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "describeQuickActions")
        self.__actions = actions

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "action", self.__actions)


class DescribeAvailableQuickActionsRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, parentType):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "describeAvailableQuickActions")
        self.__parentType = parentType

    def writeBody(self, s):
        s.writeStringElement(_partnerNs, "parentType", self.__parentType)


class PerformQuickActionsRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, actions):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "performQuickActions")
        self.__actions = actions

    def writeBody(self, s):
        if (islst(self.__actions)):
            for action in self.__actions:
                self.writeQuckAction(s, action)
        else:
            self.writeQuickAction(s, self.__actions)

    def writeQuickAction(self, s, action):
        s.startElement(_partnerNs, "quickActions")
        s.writeStringElement(_partnerNs, "parentId", action.get("parentId"))
        s.writeStringElement(_partnerNs, "quickActionName", action["quickActionName"])
        self.writeSObjects(s, action["records"], "records")
        s.endElement()


class CompileClassRequest(AuthenticatedRequest):
    def __init__(self, serverUrl, sessionId, scripts):
        AuthenticatedRequest.__init__(self, serverUrl, sessionId, "compileClasses")
        self.__scripts = scripts

    def writeBody(self, s):
        s.writeStringElement(_apexNs, "scripts", self.__scripts)
