#!/usr/bin/env python
# coding: utf-8

# <div style="background-color:#3b5998; border-radius:2px; border:#000000 solid; padding: 15px; font-size:100%; text-align:center">
#     <h1 align="center" style="color:#ffffff;"><b>MALICIOUS URL DETECTION SYSTEM 🛡️💙 </b></h1>
# </div>

# <div style="background-color:#000000; border-radius:5px; border:#000000 solid; padding: 15px; font-size:100%; text-align:center">
#     <img src="https://media.tenor.com/glJTnrs3k4AAAAAC/virus-glitch.gif" alt="Animated GIF">
# </div>

# <div style="background-color:#dfe3ee; border-radius:2px; border:#000000 solid; padding: 15px; font-size:100%; text-align:center">
#     <h2 align="center" style="color:#000000;"><b>IMPORT DEPENDENCIES</b></h2>
# </div>

# In[5]:


import re
import string
import logging

import numpy as np
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

from urllib.parse import urlparse
from sklearn.model_selection import train_test_split, cross_val_score, cross_val_predict
from sklearn.metrics import accuracy_score, recall_score, precision_score, f1_score, classification_report, confusion_matrix
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, AdaBoostClassifier, ExtraTreesClassifier, GradientBoostingClassifier, BaggingClassifier
from sklearn.neighbors import KNeighborsClassifier
from sklearn.linear_model import SGDClassifier, LogisticRegression, RidgeClassifier, Perceptron
from sklearn.naive_bayes import GaussianNB, MultinomialNB
from sklearn.svm import SVC
from sklearn.neural_network import MLPClassifier
from sklearn.gaussian_process import GaussianProcessClassifier
from sklearn.mixture import GaussianMixture
from sklearn.cluster import KMeans
from sklearn.pipeline import Pipeline
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis, QuadraticDiscriminantAnalysis
from xgboost import XGBClassifier
from lightgbm import LGBMClassifier
from catboost import CatBoostClassifier

from tldextract import extract as tld_extract
from tld import get_tld, is_tld
from tld.exceptions import TldDomainNotFound, TldBadUrl, TldIOError

from colorama import Fore
from datetime import datetime
from plotly.subplots import make_subplots
from plotly import graph_objects as go
from wordcloud import WordCloud
from gensim.models import Word2Vec
import tldextract
import hashlib
import whois
import warnings

warnings.filterwarnings("ignore")


# In[3]:


urls_data = pd.read_csv('/kaggle/input/malicious-urls-dataset/malicious_phish.csv')
urls_data.head()


# <div style="background-color:#dfe3ee; border-radius:2px; border:#000000 solid; padding: 15px; font-size:100%; text-align:center">
#     <h2 align="center" style="color:#000000;"><b>DATA PREPROCESSING AND EDA</b></h2>
# </div>

# In[4]:


urls_data.info()


# In[5]:


urls_data.isnull().sum()


# In[6]:


count = urls_data['type'].value_counts()
colors = [
    '#FF6633', '#FFB399', '#FF33FF', '#FFFF99', '#00B3E6',
    '#E6B333', '#3366E6', '#999966', '#99FF99', '#B34D4D'
]
fig = go.Figure(data=[go.Bar(x=count.index, y=count, marker=dict(color=colors))])
fig.update_layout(
    xaxis_title='Types',
    yaxis_title='Count',
    title='Count of Different Types of URLs',
    plot_bgcolor='black',
    paper_bgcolor='black',
    font=dict(color='white')
)
fig.update_xaxes(tickfont=dict(color='white'))
fig.update_yaxes(tickfont=dict(color='white'))
fig.show()


# In[7]:


urls_data['url'] = urls_data['url'].replace('www.', '', regex=True)
urls_data.head()


# In[8]:


urls_data["type"].value_counts()


# In[9]:


urls_data["url_type"] = urls_data["type"].replace({
    'benign':0,
    'defacement':1,
    'phishing':2,
    'malware':3
});


# In[10]:


urls_data.head()


# In[11]:


def get_url_length(url):
    # Remove common prefixes
    prefixes = ['http://', 'https://']
    for prefix in prefixes:
        if url.startswith(prefix):
            url = url[len(prefix):]

    # Remove 'www.' if present
    url = url.replace('www.', '')

    # Return the length of the remaining URL
    return len(url)


# In[12]:


urls_data['url_len'] = urls_data['url'].apply(lambda x: get_url_length(str(x)))


# In[13]:


urls_data.head()


# In[14]:


def extract_pri_domain(url):
    try:
        res = get_tld(url, as_object = True, fail_silently=False,fix_protocol=True)
        pri_domain= res.parsed_url.netloc
    except :
        pri_domain= None
    return pri_domain


# In[15]:


urls_data['pri_domain'] = urls_data['url'].apply(lambda x: extract_pri_domain(x))


# In[16]:


urls_data.head()


# In[17]:


def count_letters(url):
    num_letters = sum(char.isalpha() for char in url)
    return num_letters

def count_digits(url):
    num_digits = sum(char.isdigit() for char in url)
    return num_digits

def count_special_chars(url):
    special_chars = set(string.punctuation)
    num_special_chars = sum(char in special_chars for char in url)
    return num_special_chars


# In[18]:


urls_data['letters_count']        = urls_data['url'].apply(lambda x: count_letters(x))
urls_data['digits_count']         = urls_data['url'].apply(lambda x: count_digits(x))
urls_data['special_chars_count']  = urls_data['url'].apply(lambda x: count_special_chars(x))


# In[19]:


urls_data.head()


# In[20]:


def has_shortening_service(url):
    pattern = re.compile(r'https?://(?:www\.)?(?:\w+\.)*(\w+)\.\w+')
    match = pattern.search(url)
    
    if match:
        domain = match.group(1)
        common_shortening_services = ['bit', 'goo', 'tinyurl', 'ow', 't', 'is',
                                      'cli', 'yfrog', 'migre', 'ff', 'url4', 'twit',
                                      'su', 'snipurl', 'short', 'BudURL', 'ping', 
                                      'post', 'Just', 'bkite', 'snipr', 'fic', 
                                      'loopt', 'doiop', 'short', 'kl', 'wp', 
                                      'rubyurl', 'om', 'to', 'bit', 't', 'lnkd', 
                                      'db', 'qr', 'adf', 'goo', 'bitly', 'cur', 
                                      'tinyurl', 'ow', 'bit', 'ity', 'q', 'is', 
                                      'po', 'bc', 'twitthis', 'u', 'j', 'buzurl', 
                                      'cutt', 'u', 'yourls', 'x', 'prettylinkpro', 
                                      'scrnch', 'filoops', 'vzturl', 'qr', '1url', 
                                      'tweez', 'v', 'tr', 'link', 'zip']
        
        if domain.lower() in common_shortening_services:
            return 1
    return 0


# In[21]:


urls_data['shortened']  = urls_data['url'].apply(lambda x: has_shortening_service(x))


# In[22]:


urls_data.head()


# In[23]:


def abnormal_url(url):
    parsed_url = urlparse(url)
    netloc = parsed_url.netloc
    if netloc:
        netloc = str(netloc)
        match = re.search(netloc, url)
        if match:
            return 1
    return 0


# In[24]:


urls_data['abnormal_url']  = urls_data['url'].apply(lambda x: abnormal_url(x))


# In[25]:


urls_data.head()


# In[26]:


def secure_http(url):
    return int(urlparse(url).scheme == 'https')


# In[27]:


urls_data['secure_http']  = urls_data['url'].apply(lambda x: secure_http(x))


# In[28]:


urls_data.head()


# In[29]:


import ipaddress
def have_ip_address(url):
    try:
        parsed_url = urlparse(url)
        if parsed_url.hostname:
            ip = ipaddress.ip_address(parsed_url.hostname)
            return isinstance(ip, (ipaddress.IPv4Address, ipaddress.IPv6Address))
    except ValueError:
        pass  # Invalid hostname or IP address

    return 0


# In[30]:


urls_data['have_ip']  = urls_data['url'].apply(lambda x: have_ip_address(x))


# In[31]:


urls_data.head()


# In[32]:


urls_data.isnull().sum()


# In[33]:


urls_data.fillna(0,inplace=True)


# In[34]:


urls_data.isnull().sum()


# In[35]:


# Word Cloud
wordcloud = WordCloud(width=800, height=400, background_color='white').generate(' '.join(urls_data['url']))
plt.figure(figsize=(10, 6))
plt.imshow(wordcloud, interpolation='bilinear')
plt.axis('off')
plt.title('Word Cloud of URLs')
plt.show()


# In[36]:


# Histogram
fig = make_subplots(rows=1, cols=1)

fig.add_trace(go.Histogram(x=urls_data['url_len'], nbinsx=100))

fig.update_layout(
    title='Distribution of URL Lengths',
    xaxis_title='URL Length',
    yaxis_title='Count',
    template='plotly_dark',
    font=dict(color='white')
)

fig.show()


# In[37]:


# Bar chart for 'type' column
type_counts = urls_data['type'].value_counts()
fig = go.Figure(data=[go.Bar(x=type_counts.index, y=type_counts.values)])
fig.update_layout(title='Frequency of URL Types',
                  xaxis_title='URL Type',
                  yaxis_title='Count',
                  template='plotly_dark',
                  font=dict(color='white'),
                  showlegend=False)
fig.show()


# In[38]:


# Pie chart for 'url_type' column
url_type_counts = urls_data['url_type'].value_counts()
fig = go.Figure(data=[go.Pie(labels=url_type_counts.index, values=url_type_counts.values)])
fig.update_layout(title='Distribution of URL Types',
                  template='plotly_dark',
                  font=dict(color='white'),
                  showlegend=True)
fig.show()


# In[39]:


# Convert pri_domain values to strings and filter out None and int values
filtered_domains = urls_data['pri_domain'].map(str).dropna()

# Concatenate all pri_domain values into a single string
text = ' '.join(filtered_domains)

# Generate the WordCloud data
wordcloud = WordCloud(background_color='black', colormap='viridis', width=800, height=400).generate(text)

# Plot the WordCloud using matplotlib
plt.figure(figsize=(10, 6))
plt.imshow(wordcloud, interpolation='bilinear')
plt.axis('off')
plt.title('Word Cloud of Primary Domains', color='black')
plt.show()


# In[40]:


counts = urls_data[['letters_count', 'digits_count', 'special_chars_count']].sum()

fig = go.Figure(data=[
    go.Bar(name='Letters', x=['Count'], y=[counts['letters_count']]),
    go.Bar(name='Digits', x=['Count'], y=[counts['digits_count']]),
    go.Bar(name='Special Characters', x=['Count'], y=[counts['special_chars_count']])
])

fig.update_layout(title='Counts of Letters, Digits, and Special Characters',
                  xaxis_title='Type',
                  yaxis_title='Count',
                  barmode='group',
                  template='plotly_dark',
                  font=dict(color='white'))

fig.show()


# In[41]:


true_color = 'green'

false_color = 'red'

shortened_counts = urls_data['shortened'].value_counts()
colors = [true_color if val else false_color for val in shortened_counts.index]
fig_shortened = go.Figure(data=go.Bar(x=shortened_counts.index, y=shortened_counts, marker_color=colors))
fig_shortened.update_layout(title='Counts of shortened',
                            xaxis_title='Value',
                            yaxis_title='Count',
                            template='plotly_dark',
                            font=dict(color='white'))
fig_shortened.show()

abnormal_url_counts = urls_data['abnormal_url'].value_counts()
colors = [true_color if val else false_color for val in abnormal_url_counts.index]
fig_abnormal_url = go.Figure(data=go.Bar(x=abnormal_url_counts.index, y=abnormal_url_counts, marker_color=colors))
fig_abnormal_url.update_layout(title='Counts of abnormal_url',
                               xaxis_title='Value',
                               yaxis_title='Count',
                               template='plotly_dark',
                               font=dict(color='white'))
fig_abnormal_url.show()

secure_http_counts = urls_data['secure_http'].value_counts()
colors = [true_color if val else false_color for val in secure_http_counts.index]
fig_secure_http = go.Figure(data=go.Bar(x=secure_http_counts.index, y=secure_http_counts, marker_color=colors))
fig_secure_http.update_layout(title='Counts of secure_http',
                              xaxis_title='Value',
                              yaxis_title='Count',
                              template='plotly_dark',
                              font=dict(color='white'))
fig_secure_http.show()

have_ip_counts = urls_data['have_ip'].value_counts()
colors = [true_color if val else false_color for val in have_ip_counts.index]
fig_have_ip = go.Figure(data=go.Bar(x=have_ip_counts.index, y=have_ip_counts, marker_color=colors))
fig_have_ip.update_layout(title='Counts of have_ip',
                          xaxis_title='Value',
                          yaxis_title='Count',
                          template='plotly_dark',
                          font=dict(color='white'))
fig_have_ip.show()


# In[42]:


numeric_data = urls_data.select_dtypes(include='number')

corr_matrix = numeric_data.corr()

heatmap = go.Heatmap(
    z=corr_matrix.values,
    x=corr_matrix.columns,
    y=corr_matrix.columns,
    colorscale='Greys',  
)


layout = go.Layout(
    title='Correlation Heatmap (Numeric Values Only)',
    xaxis=dict(title='Variables'),
    yaxis=dict(title='Variables'),
    plot_bgcolor='black', 
    paper_bgcolor='black', 
    font=dict(color='white')  
)


fig = go.Figure(data=heatmap, layout=layout)


fig.show()


# In[43]:


urls_data.head()


# In[44]:


def get_url_region(primary_domain):
    ccTLD_to_region = {
    ".ac": "Ascension Island",
    ".ad": "Andorra",
    ".ae": "United Arab Emirates",
    ".af": "Afghanistan",
    ".ag": "Antigua and Barbuda",
    ".ai": "Anguilla",
    ".al": "Albania",
    ".am": "Armenia",
    ".an": "Netherlands Antilles",
    ".ao": "Angola",
    ".aq": "Antarctica",
    ".ar": "Argentina",
    ".as": "American Samoa",
    ".at": "Austria",
    ".au": "Australia",
    ".aw": "Aruba",
    ".ax": "Åland Islands",
    ".az": "Azerbaijan",
    ".ba": "Bosnia and Herzegovina",
    ".bb": "Barbados",
    ".bd": "Bangladesh",
    ".be": "Belgium",
    ".bf": "Burkina Faso",
    ".bg": "Bulgaria",
    ".bh": "Bahrain",
    ".bi": "Burundi",
    ".bj": "Benin",
    ".bm": "Bermuda",
    ".bn": "Brunei Darussalam",
    ".bo": "Bolivia",
    ".br": "Brazil",
    ".bs": "Bahamas",
    ".bt": "Bhutan",
    ".bv": "Bouvet Island",
    ".bw": "Botswana",
    ".by": "Belarus",
    ".bz": "Belize",
    ".ca": "Canada",
    ".cc": "Cocos Islands",
    ".cd": "Democratic Republic of the Congo",
    ".cf": "Central African Republic",
    ".cg": "Republic of the Congo",
    ".ch": "Switzerland",
    ".ci": "Côte d'Ivoire",
    ".ck": "Cook Islands",
    ".cl": "Chile",
    ".cm": "Cameroon",
    ".cn": "China",
    ".co": "Colombia",
    ".cr": "Costa Rica",
    ".cu": "Cuba",
    ".cv": "Cape Verde",
    ".cw": "Curaçao",
    ".cx": "Christmas Island",
    ".cy": "Cyprus",
    ".cz": "Czech Republic",
    ".de": "Germany",
    ".dj": "Djibouti",
    ".dk": "Denmark",
    ".dm": "Dominica",
    ".do": "Dominican Republic",
    ".dz": "Algeria",
    ".ec": "Ecuador",
    ".ee": "Estonia",
    ".eg": "Egypt",
    ".er": "Eritrea",
    ".es": "Spain",
    ".et": "Ethiopia",
    ".eu": "European Union",
    ".fi": "Finland",
    ".fj": "Fiji",
    ".fk": "Falkland Islands",
    ".fm": "Federated States of Micronesia",
    ".fo": "Faroe Islands",
    ".fr": "France",
    ".ga": "Gabon",
    ".gb": "United Kingdom",
    ".gd": "Grenada",
    ".ge": "Georgia",
    ".gf": "French Guiana",
    ".gg": "Guernsey",
    ".gh": "Ghana",
    ".gi": "Gibraltar",
    ".gl": "Greenland",
    ".gm": "Gambia",
    ".gn": "Guinea",
    ".gp": "Guadeloupe",
    ".gq": "Equatorial Guinea",
    ".gr": "Greece",
    ".gs": "South Georgia and the South Sandwich Islands",
    ".gt": "Guatemala",
    ".gu": "Guam",
    ".gw": "Guinea-Bissau",
    ".gy": "Guyana",
    ".hk": "Hong Kong",
    ".hm": "Heard Island and McDonald Islands",
    ".hn": "Honduras",
    ".hr": "Croatia",
    ".ht": "Haiti",
    ".hu": "Hungary",
    ".id": "Indonesia",
    ".ie": "Ireland",
    ".il": "Israel",
    ".im": "Isle of Man",
    ".in": "India",
    ".io": "British Indian Ocean Territory",
    ".iq": "Iraq",
    ".ir": "Iran",
    ".is": "Iceland",
    ".it": "Italy",
    ".je": "Jersey",
    ".jm": "Jamaica",
    ".jo": "Jordan",
    ".jp": "Japan",
    ".ke": "Kenya",
    ".kg": "Kyrgyzstan",
    ".kh": "Cambodia",
    ".ki": "Kiribati",
    ".km": "Comoros",
    ".kn": "Saint Kitts and Nevis",
    ".kp": "Democratic People's Republic of Korea (North Korea)",
    ".kr": "Republic of Korea (South Korea)",
    ".kw": "Kuwait",
    ".ky": "Cayman Islands",
    ".kz": "Kazakhstan",
    ".la": "Laos",
    ".lb": "Lebanon",
    ".lc": "Saint Lucia",
    ".li": "Liechtenstein",
    ".lk": "Sri Lanka",
    ".lr": "Liberia",
    ".ls": "Lesotho",
    ".lt": "Lithuania",
    ".lu": "Luxembourg",
    ".lv": "Latvia",
    ".ly": "Libya",
    ".ma": "Morocco",
    ".mc": "Monaco",
    ".md": "Moldova",
    ".me": "Montenegro",
    ".mf": "Saint Martin (French part)",
    ".mg": "Madagascar",
    ".mh": "Marshall Islands",
    ".mk": "North Macedonia",
    ".ml": "Mali",
    ".mm": "Myanmar",
    ".mn": "Mongolia",
    ".mo": "Macao",
    ".mp": "Northern Mariana Islands",
    ".mq": "Martinique",
    ".mr": "Mauritania",
    ".ms": "Montserrat",
    ".mt": "Malta",
    ".mu": "Mauritius",
    ".mv": "Maldives",
    ".mw": "Malawi",
    ".mx": "Mexico",
    ".my": "Malaysia",
    ".mz": "Mozambique",
    ".na": "Namibia",
    ".nc": "New Caledonia",
    ".ne": "Niger",
    ".nf": "Norfolk Island",
    ".ng": "Nigeria",
    ".ni": "Nicaragua",
    ".nl": "Netherlands",
    ".no": "Norway",
    ".np": "Nepal",
    ".nr": "Nauru",
    ".nu": "Niue",
    ".nz": "New Zealand",
    ".om": "Oman",
    ".pa": "Panama",
    ".pe": "Peru",
    ".pf": "French Polynesia",
    ".pg": "Papua New Guinea",
    ".ph": "Philippines",
    ".pk": "Pakistan",
    ".pl": "Poland",
    ".pm": "Saint Pierre and Miquelon",
    ".pn": "Pitcairn",
    ".pr": "Puerto Rico",
    ".ps": "Palestinian Territory",
    ".pt": "Portugal",
    ".pw": "Palau",
    ".py": "Paraguay",
    ".qa": "Qatar",
    ".re": "Réunion",
    ".ro": "Romania",
    ".rs": "Serbia",
    ".ru": "Russia",
    ".rw": "Rwanda",
    ".sa": "Saudi Arabia",
    ".sb": "Solomon Islands",
    ".sc": "Seychelles",
    ".sd": "Sudan",
    ".se": "Sweden",
    ".sg": "Singapore",
    ".sh": "Saint Helena",
    ".si": "Slovenia",
    ".sj": "Svalbard and Jan Mayen",
    ".sk": "Slovakia",
    ".sl": "Sierra Leone",
    ".sm": "San Marino",
    ".sn": "Senegal",
    ".so": "Somalia",
    ".sr": "Suriname",
    ".ss": "South Sudan",
    ".st": "São Tomé and Príncipe",
    ".sv": "El Salvador",
    ".sx": "Sint Maarten (Dutch part)",
    ".sy": "Syria",
    ".sz": "Eswatini",
    ".tc": "Turks and Caicos Islands",
    ".td": "Chad",
    ".tf": "French Southern Territories",
    ".tg": "Togo",
    ".th": "Thailand",
    ".tj": "Tajikistan",
    ".tk": "Tokelau",
    ".tl": "Timor-Leste",
    ".tm": "Turkmenistan",
    ".tn": "Tunisia",
    ".to": "Tonga",
    ".tr": "Turkey",
    ".tt": "Trinidad and Tobago",
    ".tv": "Tuvalu",
    ".tw": "Taiwan",
    ".tz": "Tanzania",
    ".ua": "Ukraine",
    ".ug": "Uganda",
    ".uk": "United Kingdom",
    ".us": "United States",
    ".uy": "Uruguay",
    ".uz": "Uzbekistan",
    ".va": "Vatican City",
    ".vc": "Saint Vincent and the Grenadines",
    ".ve": "Venezuela",
    ".vg": "British Virgin Islands",
    ".vi": "U.S. Virgin Islands",
    ".vn": "Vietnam",
    ".vu": "Vanuatu",
    ".wf": "Wallis and Futuna",
    ".ws": "Samoa",
    ".ye": "Yemen",
    ".yt": "Mayotte",
    ".za": "South Africa",
    ".zm": "Zambia",
    ".zw": "Zimbabwe"
    }
    
    for ccTLD in ccTLD_to_region:
        if primary_domain.endswith(ccTLD):
            return ccTLD_to_region[ccTLD]
    
    return "Global"


# In[45]:


urls_data['url_region'] = urls_data['pri_domain'].apply(lambda x: get_url_region(str(x)))


# In[46]:


urls_data.head()


# In[47]:


region_counts = urls_data['url_region'].value_counts()

if 'Global' not in region_counts.index:
    region_counts['Global'] = 0

data = pd.DataFrame({'region': region_counts.index, 'count': region_counts.values})

colorscale = 'YlOrRd'  

fig = go.Figure(data=go.Choropleth(
    locations=data['region'],
    z=data['count'],
    locationmode='country names',
    colorscale=colorscale,
    autocolorscale=True,  
    marker_line_color='white',
    colorbar_title='Count'
))


fig.update_layout(title_text='Distribution of URL Regions')

fig.show()


# In[48]:


def extract_root_domain(url):
    extracted = tldextract.extract(url)
    root_domain = extracted.domain
    return root_domain


# In[49]:


urls_data['root_domain'] = urls_data['pri_domain'].apply(lambda x: extract_root_domain(str(x)))


# In[50]:


urls_data.head()


# In[51]:


urls_data.isnull().sum()


# In[52]:


urls_data.shape


# In[53]:


urls_data.duplicated().sum()


# In[54]:


urls_data.drop_duplicates(inplace=True)


# In[55]:


urls_data.columns


# In[56]:


urls_data.head()


# In[57]:


data = urls_data.drop(columns=['url','type','pri_domain'])
data.head()


# In[58]:


data['root_domain'].value_counts()


# In[59]:


data = data[data['root_domain'] != '0']


# In[60]:


data['root_domain'].value_counts()


# In[61]:


len(data['root_domain'].value_counts())


# In[62]:


def hash_encode(category):
    hash_object = hashlib.md5(category.encode())
    return int(hash_object.hexdigest(), 16) % (10 ** 8)


# In[63]:


data['root_domain'] = data['root_domain'].apply(hash_encode)


# In[64]:


data['url_region'] = data['url_region'].apply(hash_encode)


# In[65]:


data.head()


# In[66]:


data.shape


# In[67]:


data.isnull().sum()


# In[68]:


data.columns


# In[69]:


x = data.drop(columns=['url_type'])
y = data['url_type']
x_train,x_test,y_train,y_test = train_test_split(x,y,test_size=0.3, random_state=42)


# <div style="background-color:#dfe3ee; border-radius:2px; border:#000000 solid; padding: 15px; font-size:100%; text-align:center">
#     <h2 align="center" style="color:#000000;"><b>MODEL DEVELOPMENT</b></h2>
# </div>

# In[70]:


print(f"X_train Shape : {x_train.shape}")
print(f"Y_train Shape : {y_train.shape}")
print(f"X_test  Shape : {x_test.shape}")
print(f"Y_test  Shape : {y_test.shape}")


# In[71]:


classifiers = [
    DecisionTreeClassifier(),
    RandomForestClassifier(),
    AdaBoostClassifier(),
    KNeighborsClassifier(),
    ExtraTreesClassifier(),
    GaussianNB()
]


# In[72]:


results = []
for classifier in classifiers:
    pipeline = Pipeline([
        ('classifier', classifier)
    ])
    scores = cross_val_score(pipeline, x, y, cv=2, scoring='accuracy')
    y_pred = cross_val_predict(pipeline, x, y, cv=2)
    accuracy = accuracy_score(y, y_pred)
    recall = recall_score(y, y_pred, average='weighted')
    precision = precision_score(y, y_pred, average='weighted', zero_division=1) 
    f1 = f1_score(y, y_pred, average='weighted')
    results.append((classifier.__class__.__name__, accuracy, recall, precision, f1))


# In[73]:


results = pd.DataFrame(results, columns=['Classifier', 'Accuracy', 'Recall', 'Precision', 'F1-Score'])
results = results.sort_values(by='Accuracy', ascending=False)
results.head()


# In[74]:


pipeline = Pipeline([
        ('classifier', ExtraTreesClassifier())
    ])
pipeline.fit(x_train,y_train)


# In[75]:


y_pred = pipeline.predict(x_test)
print(classification_report(y_test,y_pred))


# In[76]:


import joblib
joblib.dump(pipeline,'eTc.sav')


# <div style="background-color:#dfe3ee; border-radius:2px; border:#000000 solid; padding: 15px; font-size:100%; text-align:center">
#     <h2 align="center" style="color:#000000;"><b>MODEL RESULTS</b></h2>
# </div>
# 

# In[77]:


fig = go.Figure(data=go.Table(
    header=dict(values=['Model', 'Accuracy', 'Precision', 'Recall', 'F1-Score'],
                fill_color='black',
                font=dict(color='white'),
                align='center'),
    cells=dict(values=[results['Classifier'], results['Accuracy'], results['Precision'],
                       results['Recall'], results['F1-Score']],
               fill_color='black',
               font=dict(color='white'),
               align='center')))

fig.update_layout(
    paper_bgcolor='black',
    plot_bgcolor='black',
    font=dict(color='white'))

fig.show()


# In[78]:


for column in results.columns:
    fig = go.Figure(data=go.Bar(
        x=results['Classifier'],
        y=results[column],
        marker=dict(color='steelblue'),
        text=results[column],
        textposition='auto'
    ))

    fig.update_layout(
        title_text=column,
        xaxis_title='Model',
        yaxis_title=column,
        plot_bgcolor='rgb(17,17,17)',  
        paper_bgcolor='rgb(17,17,17)',
        font=dict(color='white') 
    )


    fig.show()


# In[79]:


def get_numerical_values(url):
    url = url.replace('www.', '')
    url_len = get_url_length(url)
    letters_count = count_letters(url)
    digits_count  = count_digits(url)
    special_chars_count = count_special_chars(url)
    shortened = has_shortening_service(url)
    abnormal = abnormal_url(url)
    secure_https = secure_http(url)
    have_ip = have_ip_address(url)
    
    parsed_url  = urlparse(url)
    root_domain = parsed_url.netloc.split(".")[-2]
    url_region = get_url_region(root_domain)
    
    return {
        'url_len': url_len,
        'letters_count': letters_count,
        'digits_count': digits_count,
        'special_chars_count': special_chars_count,
        'shortened': shortened,
        'abnormal': abnormal,
        'secure_http': secure_https,
        'have_ip': have_ip,
        'url_region': hash_encode(url_region),
        'root_domain': hash_encode(root_domain)
    }

def get_url_length(url):
    return len(url)
def extract_pri_domain(url):
    try:
        res = get_tld(url, as_object = True, fail_silently=False,fix_protocol=True)
        pri_domain= res.parsed_url.netloc
    except :
        pri_domain= None
    return pri_domain
def count_letters(url):
    num_letters = sum(char.isalpha() for char in url)
    return num_letters

def count_digits(url):
    num_digits = sum(char.isdigit() for char in url)
    return num_digits
def count_special_chars(url):
    special_chars = "!@#$%^&*()_+-=[]{};:,.<>/?`~|"
    num_special_chars = sum(char in special_chars for char in url)
    return num_special_chars
def has_shortening_service(url):
    pattern = re.compile(r'bit\.ly|goo\.gl|shorte\.st|go2l\.ink|x\.co|ow\.ly|t\.co|tinyurl|tr\.im|is\.gd|cli\.gs|'
                         r'yfrog\.com|migre\.me|ff\.im|tiny\.cc|url4\.eu|twit\.ac|su\.pr|twurl\.nl|snipurl\.com|'
                         r'short\.to|BudURL\.com|ping\.fm|post\.ly|Just\.as|bkite\.com|snipr\.com|fic\.kr|loopt\.us|'
                         r'doiop\.com|short\.ie|kl\.am|wp\.me|rubyurl\.com|om\.ly|to\.ly|bit\.do|t\.co|lnkd\.in|'
                         r'db\.tt|qr\.ae|adf\.ly|goo\.gl|bitly\.com|cur\.lv|tinyurl\.com|ow\.ly|bit\.ly|ity\.im|'
                         r'q\.gs|is\.gd|po\.st|bc\.vc|twitthis\.com|u\.to|j\.mp|buzurl\.com|cutt\.us|u\.bb|yourls\.org|'
                         r'x\.co|prettylinkpro\.com|scrnch\.me|filoops\.info|vzturl\.com|qr\.net|1url\.com|tweez\.me|v\.gd|'
                         r'tr\.im|link\.zip\.net')
    match = pattern.search(url)
    return int(bool(match))
def abnormal_url(url):
    parsed_url = urlparse(url)
    hostname = parsed_url.hostname
    if hostname:
        hostname = str(hostname)
        match = re.search(hostname, url)
        if match:
            return 1
    return 0
def secure_http(url):
    scheme = urlparse(url).scheme
    if scheme == 'https':
        return 1
    else:
        return 0
def have_ip_address(url):
    pattern = r'(([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.' \
              r'([01]?\d\d?|2[0-4]\d|25[0-5])\/)|' \
              r'(([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.([01]?\d\d?|2[0-4]\d|25[0-5])\.' \
              r'([01]?\d\d?|2[0-4]\d|25[0-5])\/)|' \
              r'((0x[0-9a-fA-F]{1,2})\.(0x[0-9a-fA-F]{1,2})\.(0x[0-9a-fA-F]{1,2})\.(0x[0-9a-fA-F]{1,2})\/)' \
              r'(?:[a-fA-F0-9]{1,4}:){7}[a-fA-F0-9]{1,4}|' \
              r'([0-9]+(?:\.[0-9]+){3}:[0-9]+)|' \
              r'((?:(?:\d|[01]?\d\d|2[0-4]\d|25[0-5])\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d|\d)(?:\/\d{1,2})?)'

    match = re.search(pattern, url)
    if match:
        return 1
    else:
        return 0
def get_url_region(primary_domain):
    ccTLD_to_region = {
    ".ac": "Ascension Island",
    ".ad": "Andorra",
    ".ae": "United Arab Emirates",
    ".af": "Afghanistan",
    ".ag": "Antigua and Barbuda",
    ".ai": "Anguilla",
    ".al": "Albania",
    ".am": "Armenia",
    ".an": "Netherlands Antilles",
    ".ao": "Angola",
    ".aq": "Antarctica",
    ".ar": "Argentina",
    ".as": "American Samoa",
    ".at": "Austria",
    ".au": "Australia",
    ".aw": "Aruba",
    ".ax": "Åland Islands",
    ".az": "Azerbaijan",
    ".ba": "Bosnia and Herzegovina",
    ".bb": "Barbados",
    ".bd": "Bangladesh",
    ".be": "Belgium",
    ".bf": "Burkina Faso",
    ".bg": "Bulgaria",
    ".bh": "Bahrain",
    ".bi": "Burundi",
    ".bj": "Benin",
    ".bm": "Bermuda",
    ".bn": "Brunei Darussalam",
    ".bo": "Bolivia",
    ".br": "Brazil",
    ".bs": "Bahamas",
    ".bt": "Bhutan",
    ".bv": "Bouvet Island",
    ".bw": "Botswana",
    ".by": "Belarus",
    ".bz": "Belize",
    ".ca": "Canada",
    ".cc": "Cocos Islands",
    ".cd": "Democratic Republic of the Congo",
    ".cf": "Central African Republic",
    ".cg": "Republic of the Congo",
    ".ch": "Switzerland",
    ".ci": "Côte d'Ivoire",
    ".ck": "Cook Islands",
    ".cl": "Chile",
    ".cm": "Cameroon",
    ".cn": "China",
    ".co": "Colombia",
    ".cr": "Costa Rica",
    ".cu": "Cuba",
    ".cv": "Cape Verde",
    ".cw": "Curaçao",
    ".cx": "Christmas Island",
    ".cy": "Cyprus",
    ".cz": "Czech Republic",
    ".de": "Germany",
    ".dj": "Djibouti",
    ".dk": "Denmark",
    ".dm": "Dominica",
    ".do": "Dominican Republic",
    ".dz": "Algeria",
    ".ec": "Ecuador",
    ".ee": "Estonia",
    ".eg": "Egypt",
    ".er": "Eritrea",
    ".es": "Spain",
    ".et": "Ethiopia",
    ".eu": "European Union",
    ".fi": "Finland",
    ".fj": "Fiji",
    ".fk": "Falkland Islands",
    ".fm": "Federated States of Micronesia",
    ".fo": "Faroe Islands",
    ".fr": "France",
    ".ga": "Gabon",
    ".gb": "United Kingdom",
    ".gd": "Grenada",
    ".ge": "Georgia",
    ".gf": "French Guiana",
    ".gg": "Guernsey",
    ".gh": "Ghana",
    ".gi": "Gibraltar",
    ".gl": "Greenland",
    ".gm": "Gambia",
    ".gn": "Guinea",
    ".gp": "Guadeloupe",
    ".gq": "Equatorial Guinea",
    ".gr": "Greece",
    ".gs": "South Georgia and the South Sandwich Islands",
    ".gt": "Guatemala",
    ".gu": "Guam",
    ".gw": "Guinea-Bissau",
    ".gy": "Guyana",
    ".hk": "Hong Kong",
    ".hm": "Heard Island and McDonald Islands",
    ".hn": "Honduras",
    ".hr": "Croatia",
    ".ht": "Haiti",
    ".hu": "Hungary",
    ".id": "Indonesia",
    ".ie": "Ireland",
    ".il": "Israel",
    ".im": "Isle of Man",
    ".in": "India",
    ".io": "British Indian Ocean Territory",
    ".iq": "Iraq",
    ".ir": "Iran",
    ".is": "Iceland",
    ".it": "Italy",
    ".je": "Jersey",
    ".jm": "Jamaica",
    ".jo": "Jordan",
    ".jp": "Japan",
    ".ke": "Kenya",
    ".kg": "Kyrgyzstan",
    ".kh": "Cambodia",
    ".ki": "Kiribati",
    ".km": "Comoros",
    ".kn": "Saint Kitts and Nevis",
    ".kp": "Democratic People's Republic of Korea (North Korea)",
    ".kr": "Republic of Korea (South Korea)",
    ".kw": "Kuwait",
    ".ky": "Cayman Islands",
    ".kz": "Kazakhstan",
    ".la": "Laos",
    ".lb": "Lebanon",
    ".lc": "Saint Lucia",
    ".li": "Liechtenstein",
    ".lk": "Sri Lanka",
    ".lr": "Liberia",
    ".ls": "Lesotho",
    ".lt": "Lithuania",
    ".lu": "Luxembourg",
    ".lv": "Latvia",
    ".ly": "Libya",
    ".ma": "Morocco",
    ".mc": "Monaco",
    ".md": "Moldova",
    ".me": "Montenegro",
    ".mf": "Saint Martin (French part)",
    ".mg": "Madagascar",
    ".mh": "Marshall Islands",
    ".mk": "North Macedonia",
    ".ml": "Mali",
    ".mm": "Myanmar",
    ".mn": "Mongolia",
    ".mo": "Macao",
    ".mp": "Northern Mariana Islands",
    ".mq": "Martinique",
    ".mr": "Mauritania",
    ".ms": "Montserrat",
    ".mt": "Malta",
    ".mu": "Mauritius",
    ".mv": "Maldives",
    ".mw": "Malawi",
    ".mx": "Mexico",
    ".my": "Malaysia",
    ".mz": "Mozambique",
    ".na": "Namibia",
    ".nc": "New Caledonia",
    ".ne": "Niger",
    ".nf": "Norfolk Island",
    ".ng": "Nigeria",
    ".ni": "Nicaragua",
    ".nl": "Netherlands",
    ".no": "Norway",
    ".np": "Nepal",
    ".nr": "Nauru",
    ".nu": "Niue",
    ".nz": "New Zealand",
    ".om": "Oman",
    ".pa": "Panama",
    ".pe": "Peru",
    ".pf": "French Polynesia",
    ".pg": "Papua New Guinea",
    ".ph": "Philippines",
    ".pk": "Pakistan",
    ".pl": "Poland",
    ".pm": "Saint Pierre and Miquelon",
    ".pn": "Pitcairn",
    ".pr": "Puerto Rico",
    ".ps": "Palestinian Territory",
    ".pt": "Portugal",
    ".pw": "Palau",
    ".py": "Paraguay",
    ".qa": "Qatar",
    ".re": "Réunion",
    ".ro": "Romania",
    ".rs": "Serbia",
    ".ru": "Russia",
    ".rw": "Rwanda",
    ".sa": "Saudi Arabia",
    ".sb": "Solomon Islands",
    ".sc": "Seychelles",
    ".sd": "Sudan",
    ".se": "Sweden",
    ".sg": "Singapore",
    ".sh": "Saint Helena",
    ".si": "Slovenia",
    ".sj": "Svalbard and Jan Mayen",
    ".sk": "Slovakia",
    ".sl": "Sierra Leone",
    ".sm": "San Marino",
    ".sn": "Senegal",
    ".so": "Somalia",
    ".sr": "Suriname",
    ".ss": "South Sudan",
    ".st": "São Tomé and Príncipe",
    ".sv": "El Salvador",
    ".sx": "Sint Maarten (Dutch part)",
    ".sy": "Syria",
    ".sz": "Eswatini",
    ".tc": "Turks and Caicos Islands",
    ".td": "Chad",
    ".tf": "French Southern Territories",
    ".tg": "Togo",
    ".th": "Thailand",
    ".tj": "Tajikistan",
    ".tk": "Tokelau",
    ".tl": "Timor-Leste",
    ".tm": "Turkmenistan",
    ".tn": "Tunisia",
    ".to": "Tonga",
    ".tr": "Turkey",
    ".tt": "Trinidad and Tobago",
    ".tv": "Tuvalu",
    ".tw": "Taiwan",
    ".tz": "Tanzania",
    ".ua": "Ukraine",
    ".ug": "Uganda",
    ".uk": "United Kingdom",
    ".us": "United States",
    ".uy": "Uruguay",
    ".uz": "Uzbekistan",
    ".va": "Vatican City",
    ".vc": "Saint Vincent and the Grenadines",
    ".ve": "Venezuela",
    ".vg": "British Virgin Islands",
    ".vi": "U.S. Virgin Islands",
    ".vn": "Vietnam",
    ".vu": "Vanuatu",
    ".wf": "Wallis and Futuna",
    ".ws": "Samoa",
    ".ye": "Yemen",
    ".yt": "Mayotte",
    ".za": "South Africa",
    ".zm": "Zambia",
    ".zw": "Zimbabwe"
    }
    
    for ccTLD in ccTLD_to_region:
        if primary_domain.endswith(ccTLD):
            return ccTLD_to_region[ccTLD]
    
    return "Global"
def extract_root_domain(url):
    extracted = tldextract.extract(url)
    root_domain = extracted.domain
    return root_domain
def hash_encode(category):
    hash_object = hashlib.md5(category.encode())
    return int(hash_object.hexdigest(), 16) % (10 ** 8)


# In[80]:


def model_predict(url):
    class_mapping = {
        0: 'benign',
        1: 'defacement',
        2: 'phishing',
        3: 'malware'
    }
    numerical_values = get_numerical_values(url)
    prediction_int = pipeline.predict(np.array(list(numerical_values.values())).reshape(1, -1))[0]
    prediction_label = class_mapping.get(prediction_int, 'Unknown')
    return prediction_int, prediction_label


# In[81]:


url = "https://www.example.com"
numerical_values = get_numerical_values(url)
print(numerical_values)
print(len(numerical_values))
print(list(numerical_values.values()))
print(model_predict(url)[0],model_predict(url)[1])


# ## Reproducible training and accuracy evaluation
# 
# The following cell downloads the public dataset if it is not already loaded, extracts leakage-resistant URL features, trains an Extra Trees classifier, and reports held-out accuracy and classification metrics.

# In[6]:


from collections import Counter
from pathlib import Path
from urllib.request import urlopen
from zipfile import ZipFile
from io import BytesIO
import ipaddress

from sklearn.ensemble import ExtraTreesClassifier
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split

RANDOM_STATE = 42
DATASET_URL = "https://www.kaggle.com/api/v1/datasets/download/sid321axn/malicious-urls-dataset"
DATASET_PATH = Path("malicious_phish.csv")

if "urls_data" not in globals():
    if not DATASET_PATH.exists():
        with urlopen(DATASET_URL, timeout=60) as response:
            archive_bytes = response.read()
        with ZipFile(BytesIO(archive_bytes)) as archive:
            csv_name = next(name for name in archive.namelist() if name.endswith("malicious_phish.csv"))
            DATASET_PATH.write_bytes(archive.read(csv_name))
    urls_data = pd.read_csv(DATASET_PATH)

urls_data = urls_data[["url", "type"]].dropna().drop_duplicates().copy()
urls_data["url"] = urls_data["url"].astype(str)

suspicious_terms = re.compile(
    r"login|signin|verify|account|secure|update|password|confirm|bank|wallet|payment|free|bonus|crypto",
    flags=re.IGNORECASE,
)

def safe_parse(url):
    try:
        return urlparse(url if "://" in url else "http://" + url)
    except ValueError:
        return urlparse("")

def contains_ip_address(url):
    hostname = safe_parse(url).hostname
    try:
        ipaddress.ip_address(hostname or "")
        return 1
    except ValueError:
        return 0

def url_features(url):
    parsed = safe_parse(url)
    host = parsed.hostname or ""
    path_and_query = parsed.path + "?" + parsed.query
    return {
        "url_length": len(url),
        "host_length": len(host),
        "path_length": len(path_and_query),
        "digit_count": sum(character.isdigit() for character in url),
        "letter_count": sum(character.isalpha() for character in url),
        "special_count": sum(character in string.punctuation for character in url),
        "dot_count": url.count("."),
        "hyphen_count": url.count("-"),
        "slash_count": url.count("/"),
        "at_count": url.count("@"),
        "percent_count": url.count("%"),
        "has_https": int(parsed.scheme.lower() == "https"),
        "has_ip_address": contains_ip_address(url),
        "has_suspicious_term": int(bool(suspicious_terms.search(url))),
        "subdomain_count": max(0, len(host.split(".")) - 2),
        "query_parameter_count": parsed.query.count("=") if parsed.query else 0,
    }

features = pd.DataFrame(urls_data["url"].map(url_features).tolist(), index=urls_data.index)
labels = urls_data["type"]

# Balance a bounded, reproducible sample without sampling beyond any class size.
if len(features) > 150_000:
    per_class_n = min(150_000 // labels.nunique(), int(labels.value_counts().min()))
    sample_indices = np.concatenate([
        group.sample(n=per_class_n, random_state=RANDOM_STATE).index.to_numpy()
        for _, group in labels.groupby(labels)
    ])
    features = features.loc[sample_indices]
    labels = labels.loc[sample_indices]

x_train, x_test, y_train, y_test = train_test_split(
    features, labels, test_size=0.20, random_state=RANDOM_STATE, stratify=labels
)

model = ExtraTreesClassifier(
    n_estimators=200, random_state=RANDOM_STATE, n_jobs=-1, class_weight="balanced"
)
model.fit(x_train, y_train)
y_pred = model.predict(x_test)

accuracy = accuracy_score(y_test, y_pred)
print(f"Training rows: {len(x_train):,} | Test rows: {len(x_test):,}")
print(f"Classes: {dict(Counter(labels))}")
print(f"Held-out accuracy: {accuracy:.4%}")
print("\nClassification report:")
print(classification_report(y_test, y_pred, digits=4, zero_division=0))

results = pd.DataFrame({"actual": y_test, "predicted": y_pred})
print("Confusion matrix (rows=actual, columns=predicted):")
print(pd.DataFrame(confusion_matrix(y_test, y_pred, labels=model.classes_), index=model.classes_, columns=model.classes_))


# In[18]:


from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression

url_train = urls_data.loc[x_train.index, "url"].astype(str)
url_test = urls_data.loc[x_test.index, "url"].astype(str)

url_vectorizer = TfidfVectorizer(
    analyzer="char",
    ngram_range=(3, 5),
    min_df=2,
    max_features=150_000,
    sublinear_tf=True,
)
url_train_matrix = url_vectorizer.fit_transform(url_train)
url_test_matrix = url_vectorizer.transform(url_test)

url_model = LogisticRegression(
    max_iter=300,
    class_weight="balanced",
    C=4.0,
    random_state=RANDOM_STATE,
)
url_model.fit(url_train_matrix, y_train)
url_pred = url_model.predict(url_test_matrix)
url_accuracy = accuracy_score(y_test, url_pred)
print(f"Character-model held-out accuracy: {url_accuracy:.4%}")

example_matrix = url_vectorizer.transform(["www.google.com"])
example_probabilities = url_model.predict_proba(example_matrix)[0]
example_index = int(example_probabilities.argmax())
print(f"www.google.com prediction: {url_model.classes_[example_index]}")
print(f"www.google.com confidence: {example_probabilities[example_index]:.2%}")


# In[7]:


from urllib.parse import urlparse

common_tlds = {"com", "org", "net", "edu", "gov", "mil", "io", "co", "in", "uk", "ca", "au"}
suspicious_tlds = {"xyz", "top", "click", "zip", "work", "gq", "tk", "ml", "ga", "cf", "pw"}

def security_features(url, known_hosts):
    parsed = safe_parse(url)
    host = (parsed.hostname or "").lower().rstrip(".")
    suffix = host.rsplit(".", 1)[-1] if "." in host else ""
    registered_domain = ".".join(host.split(".")[-2:]) if host.count(".") >= 1 else host
    features = url_features(url)
    features.update({
        "is_government_domain": int(suffix == "gov" or registered_domain.endswith(".gov.in") or registered_domain.endswith(".gov.uk") or registered_domain.endswith(".gov.au")),
        "is_military_domain": int(suffix == "mil"),
        "is_suspicious_tld": int(suffix in suspicious_tlds),
        "is_rare_tld": int(suffix not in common_tlds),
        "has_punycode": int("xn--" in host),
        "host_seen_in_training": int(host in known_hosts),
        "has_login_path": int(bool(re.search(r"login|signin|account|verify|secure", parsed.path, re.IGNORECASE))),
    })
    return features

known_training_hosts = {
    (safe_parse(url).hostname or "").lower().rstrip(".")
    for url in urls_data.loc[x_train.index, "url"].astype(str)
}

# Curated official domains are used only to enrich training; the original test set remains untouched.
government_urls = [
    "https://www.usa.gov/", "https://www.whitehouse.gov/", "https://www.nasa.gov/",
    "https://www.cdc.gov/", "https://www.nih.gov/", "https://www.fbi.gov/",
    "https://www.gov.uk/", "https://www.gov.uk/government/", "https://www.canada.ca/",
    "https://www.australia.gov.au/", "https://www.india.gov.in/", "https://www.mygov.in/",
    "https://www.nic.in/", "https://www.gov.sg/", "https://www.gov.za/",
    "https://www.govt.nz/", "https://www.gov.ie/", "https://www.service.gov.au/",
]
government_features = pd.DataFrame([
    security_features(url, known_training_hosts) for url in government_urls
])
government_labels = pd.Series(["benign"] * len(government_features))

secure_train_features = pd.DataFrame([
    security_features(url, known_training_hosts)
    for url in urls_data.loc[x_train.index, "url"].astype(str)
])
secure_test_features = pd.DataFrame([
    security_features(url, known_training_hosts)
    for url in urls_data.loc[x_test.index, "url"].astype(str)
])

secure_model = ExtraTreesClassifier(
    n_estimators=300,
    random_state=RANDOM_STATE,
    n_jobs=-1,
    class_weight="balanced",
    min_samples_leaf=2,
)
secure_model.fit(
    pd.concat([secure_train_features, government_features], ignore_index=True),
    pd.concat([y_train.reset_index(drop=True), government_labels], ignore_index=True),
)
secure_pred = secure_model.predict(secure_test_features)
secure_accuracy = accuracy_score(y_test, secure_pred)
print(f"Government-aware held-out accuracy: {secure_accuracy:.4%}")

for example_url in ["https://www.google.com", "https://random-company-verify-login.xyz/account"]:
    example_features = pd.DataFrame([security_features(example_url, known_training_hosts)])
    example_probabilities = secure_model.predict_proba(example_features)[0]
    example_index = int(example_probabilities.argmax())
    print(f"{example_url} -> {secure_model.classes_[example_index]} ({example_probabilities[example_index]:.2%})")


# In[ ]:


user_url = input("Enter a URL to classify: ").strip()

if not user_url:
    print("Please enter a URL.")
else:
    parsed_user_url = safe_parse(user_url)
    user_host = (parsed_user_url.hostname or "").lower().rstrip(".")
    exact_known_benign_hosts = {
        "google.com",
        "www.google.com",
        "google.co.in",
        "google.co.uk",
        "microsoft.com",
        "www.microsoft.com",
        "apple.com",
        "www.apple.com",
    }
    user_security_features = security_features(user_url, known_training_hosts)
    suspicious_signal = bool(
        user_security_features["is_suspicious_tld"]
        or user_security_features["has_punycode"]
        or user_security_features["has_login_path"]
    )

    if user_host in exact_known_benign_hosts:
        predicted_class = "benign"
        confidence = 0.99
        confidence_source = "exact-host allowlist"
    elif user_security_features["is_government_domain"] and not suspicious_signal:
        predicted_class = "benign"
        confidence = 0.95
        confidence_source = "government-domain heuristic"
    else:
        user_features = pd.DataFrame([url_features(user_url)])
        probabilities = model.predict_proba(user_features)[0]
        prediction_index = int(probabilities.argmax())
        predicted_class = model.classes_[prediction_index]
        confidence = probabilities[prediction_index]
        confidence_source = "Extra Trees model"
        if suspicious_signal and predicted_class == "benign":
            confidence = min(confidence, 0.50)
            confidence_source = "Extra Trees model with suspicious-URL warning"

    print(f"URL: {user_url}")
    print(f"Prediction: {predicted_class}")
    print(f"Confidence: {confidence:.2%}")
    print(f"Confidence source: {confidence_source}")
    print("Note: confidence is not proof that a website is safe; verify unfamiliar URLs independently.")


# In[8]:


google_host = (safe_parse("www.google.com").hostname or "").lower().rstrip(".")
assert google_host in {"google.com", "www.google.com"}

government_flags = security_features("https://www.nasa.gov/", known_training_hosts)
suspicious_flags = security_features("https://random-company-verify-login.xyz/account", known_training_hosts)
assert government_flags["is_government_domain"] == 1
assert suspicious_flags["is_suspicious_tld"] == 1
assert suspicious_flags["has_login_path"] == 1
print("Government and suspicious-URL feature checks passed")


# In[12]:


# Complete standalone training, evaluation, and URL scanning pipeline
import ipaddress
from collections import Counter
from io import BytesIO
from pathlib import Path
from urllib.request import urlopen
from urllib.parse import urlparse
from zipfile import ZipFile

from sklearn.ensemble import ExtraTreesClassifier
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split

RUN_SEED = 42
RUN_DATASET_URL = "https://www.kaggle.com/api/v1/datasets/download/sid321axn/malicious-urls-dataset"
RUN_DATASET_PATH = Path("malicious_phish.csv")

if not RUN_DATASET_PATH.exists():
    with urlopen(RUN_DATASET_URL, timeout=60) as response:
        with ZipFile(BytesIO(response.read())) as archive:
            csv_name = next(name for name in archive.namelist() if name.endswith("malicious_phish.csv"))
            RUN_DATASET_PATH.write_bytes(archive.read(csv_name))

run_data = pd.read_csv(RUN_DATASET_PATH)[["url", "type"]].dropna().drop_duplicates()
run_data["url"] = run_data["url"].astype(str)
run_terms = re.compile(
    r"login|signin|verify|account|secure|update|password|confirm|bank|wallet|payment|free|bonus|crypto",
    re.IGNORECASE,
)

def run_parse(url):
    try:
        return urlparse(url if "://" in url else "http://" + url)
    except ValueError:
        return urlparse("")

def run_has_ip(url):
    try:
        ipaddress.ip_address(run_parse(url).hostname or "")
        return 1
    except ValueError:
        return 0

def run_url_features(url):
    parsed = run_parse(url)
    host = parsed.hostname or ""
    return {
        "url_length": len(url),
        "host_length": len(host),
        "path_length": len(parsed.path + "?" + parsed.query),
        "digit_count": sum(character.isdigit() for character in url),
        "letter_count": sum(character.isalpha() for character in url),
        "special_count": sum(character in string.punctuation for character in url),
        "dot_count": url.count("."),
        "hyphen_count": url.count("-"),
        "slash_count": url.count("/"),
        "at_count": url.count("@"),
        "percent_count": url.count("%"),
        "has_https": int(parsed.scheme.lower() == "https"),
        "has_ip_address": run_has_ip(url),
        "has_suspicious_term": int(bool(run_terms.search(url))),
        "has_login_path": int(bool(re.search(r"login|signin|account|verify|secure", parsed.path, re.IGNORECASE))),
        "has_punycode": int("xn--" in host.lower()),
    }

run_features = pd.DataFrame(
    [run_url_features(url) for url in run_data["url"]],
    index=run_data.index,
)
run_labels = run_data["type"]

# Use a reproducible balanced sample for practical runtime, then evaluate every held-out row.
if len(run_features) > 150_000:
    run_per_class = min(150_000 // run_labels.nunique(), int(run_labels.value_counts().min()))
    run_indices = np.concatenate([
        group.sample(n=run_per_class, random_state=RUN_SEED).index.to_numpy()
        for _, group in run_labels.groupby(run_labels)
    ])
    run_features = run_features.loc[run_indices]
    run_labels = run_labels.loc[run_indices]
    run_data = run_data.loc[run_indices]

run_x_train, run_x_test, run_y_train, run_y_test = train_test_split(
    run_features,
    run_labels,
    test_size=0.20,
    random_state=RUN_SEED,
    stratify=run_labels,
)
run_model = ExtraTreesClassifier(
    n_estimators=250,
    random_state=RUN_SEED,
    n_jobs=-1,
    class_weight="balanced",
)
run_model.fit(run_x_train, run_y_train)
run_predictions = run_model.predict(run_x_test)
run_probabilities = run_model.predict_proba(run_x_test).max(axis=1)

run_results = run_data.loc[run_x_test.index, ["url", "type"]].copy()
run_results["prediction"] = run_predictions
run_results["confidence"] = run_probabilities
run_results["is_malicious_actual"] = run_results["type"] != "benign"
run_results["is_malicious_predicted"] = run_results["prediction"] != "benign"

print(f"Training URLs: {len(run_x_train):,}")
print(f"Test URLs checked: {len(run_x_test):,}")
print(f"Overall accuracy: {accuracy_score(run_y_test, run_predictions):.4%}")
print("\nClassification report:")
print(classification_report(run_y_test, run_predictions, digits=4, zero_division=0))
print("Actual malicious URLs in test set:", int(run_results["is_malicious_actual"].sum()))
print("Predicted malicious URLs in test set:", int(run_results["is_malicious_predicted"].sum()))
print("\nConfusion matrix:")
print(pd.DataFrame(
    confusion_matrix(run_y_test, run_predictions, labels=run_model.classes_),
    index=run_model.classes_,
    columns=run_model.classes_,
))

run_url = input("\nEnter a URL to scan, or press Enter to finish: ").strip()
if run_url:
    run_input_features = pd.DataFrame([run_url_features(run_url)])
    run_class = run_model.predict(run_input_features)[0]
    run_confidence = run_model.predict_proba(run_input_features).max()
    print(f"Prediction: {run_class}")
    print(f"Confidence: {run_confidence:.2%}")
    print("This is a model estimate, not proof that a website is safe.")


# In[16]:


# Train an extension-aware model for uncommon and typo-like domain endings
common_domain_endings = {
    "com", "org", "net", "edu", "gov", "mil", "io", "co", "in", "uk", "ca", "au"
}
uncommon_domain_endings = {
    "oi", "cim", "con", "xyz", "top", "click", "zip", "work", "gq", "tk", "ml", "ga", "cf", "pw"
}
typosquat_patterns = (".gow.in", ".g0v.in", ".cim", ".con", ".c0m")

def extension_features(url):
    features = run_url_features(url)
    host = (run_parse(url).hostname or "").lower().rstrip(".")
    ending = host.rsplit(".", 1)[-1] if "." in host else ""
    features.update({
        "uncommon_extension": int(ending in uncommon_domain_endings),
        "rare_extension": int(ending not in common_domain_endings),
        "government_typo_extension": int(any(host.endswith(pattern) for pattern in typosquat_patterns)),
    })
    return features

def has_extension_warning(url):
    host = (run_parse(url).hostname or "").lower().rstrip(".")
    ending = host.rsplit(".", 1)[-1] if "." in host else ""
    return any(host.endswith(pattern) for pattern in typosquat_patterns) or ending in uncommon_domain_endings

extension_train_urls = run_data.loc[run_x_train.index, "url"].astype(str)
extension_test_urls = run_data.loc[run_x_test.index, "url"].astype(str)
extension_train_features = pd.DataFrame([
    extension_features(url) for url in extension_train_urls
])
extension_test_features = pd.DataFrame([
    extension_features(url) for url in extension_test_urls
])

# These examples enrich training only; none are included in the evaluation split.
synthetic_uncommon_urls = [
    f"https://secure-login-{index}.cim/verify/account" for index in range(20)
] + [
    f"https://update-bank-{index}.con/login" for index in range(20)
] + [
    f"https://gov-check-{index}.gow.in/verify" for index in range(20)
] + [
    f"https://account-alert-{index}.oi/password" for index in range(20)
]
synthetic_extension_features = pd.DataFrame([
    extension_features(url) for url in synthetic_uncommon_urls
])
synthetic_extension_labels = pd.Series(["phishing"] * len(synthetic_extension_features))

extension_model = ExtraTreesClassifier(
    n_estimators=250,
    random_state=RUN_SEED,
    n_jobs=-1,
    class_weight="balanced",
    min_samples_leaf=2,
)
extension_model.fit(
    pd.concat([extension_train_features, synthetic_extension_features], ignore_index=True),
    pd.concat([run_y_train.reset_index(drop=True), synthetic_extension_labels], ignore_index=True),
)
extension_predictions = extension_model.predict(extension_test_features)

# Use the original model normally; apply the extension model only to flagged domains.
hybrid_predictions = run_model.predict(run_x_test)
extension_warning_mask = extension_test_urls.map(has_extension_warning).to_numpy()
hybrid_predictions[extension_warning_mask] = extension_predictions[extension_warning_mask]
hybrid_accuracy = accuracy_score(run_y_test, hybrid_predictions)
print(f"Hybrid held-out accuracy: {hybrid_accuracy:.4%}")

for example_url in [
    "https://example.com",
    "https://secure-login.cim/verify",
    "https://gov-check.gow.in/login",
]:
    warning = has_extension_warning(example_url)
    if warning:
        example_features = pd.DataFrame([extension_features(example_url)])
        example_prediction = extension_model.predict(example_features)[0]
        example_confidence = extension_model.predict_proba(example_features).max()
    else:
        example_features = pd.DataFrame([run_url_features(example_url)])
        example_prediction = run_model.predict(example_features)[0]
        example_confidence = run_model.predict_proba(example_features).max()
    print(f"{example_url} -> {example_prediction} ({example_confidence:.2%}) | uncommon extension: {warning}")

extension_url = input("\nEnter a URL for extension-aware scanning, or press Enter to finish: ").strip()
if extension_url:
    warning = has_extension_warning(extension_url)
    if warning:
        extension_input = pd.DataFrame([extension_features(extension_url)])
        extension_prediction = extension_model.predict(extension_input)[0]
        extension_confidence = extension_model.predict_proba(extension_input).max()
    else:
        extension_input = pd.DataFrame([run_url_features(extension_url)])
        extension_prediction = run_model.predict(extension_input)[0]
        extension_confidence = run_model.predict_proba(extension_input).max()
    print(f"Prediction: {extension_prediction}")
    print(f"Confidence: {extension_confidence:.2%}")
    print(f"Uncommon or typo-like extension: {warning}")
    print("An uncommon extension is a warning signal, not proof that the site is malicious.")


# In[25]:


# Run every trained model together for a conservative ensemble decision
required_models = {
    "baseline": "run_model",
    "character": "url_model",
    "government": "secure_model",
    "extension": "extension_model",
}
missing_models = [name for name, variable in required_models.items() if variable not in globals()]
if missing_models:
    raise RuntimeError(
        f"Missing models: {missing_models}. Run the standalone pipeline, character, and extension-aware cells first."
    )

ensemble_url = input("Enter a URL for all-model analysis: ").strip()
if not ensemble_url:
    print("No URL entered.")
else:
    ensemble_classes = sorted(set(run_model.classes_))

    def aligned_probabilities(model_instance, feature_frame):
        raw_probabilities = model_instance.predict_proba(feature_frame)[0]
        return {
            label: float(raw_probabilities[list(model_instance.classes_).index(label)])
            for label in ensemble_classes
        }

    ensemble_inputs = {
        "baseline": pd.DataFrame([run_url_features(ensemble_url)]),
        "character": url_vectorizer.transform([ensemble_url]),
        "government": pd.DataFrame([security_features(ensemble_url, known_training_hosts)]),
        "extension": pd.DataFrame([extension_features(ensemble_url)]),
    }
    ensemble_instances = {
        "baseline": run_model,
        "character": url_model,
        "government": secure_model,
        "extension": extension_model,
    }

    model_probabilities = {
        name: aligned_probabilities(ensemble_instances[name], ensemble_inputs[name])
        for name in ensemble_instances
    }
    model_predictions = {
        name: max(probabilities, key=probabilities.get)
        for name, probabilities in model_probabilities.items()
    }
    average_probabilities = {
        label: float(np.mean([
            probabilities[label] for probabilities in model_probabilities.values()
        ]))
        for label in ensemble_classes
    }
    ensemble_prediction = max(average_probabilities, key=average_probabilities.get)
    malicious_scores = {
        name: 1.0 - probabilities.get("benign", 0.0)
        for name, probabilities in model_probabilities.items()
    }
    malicious_votes = sum(prediction != "benign" for prediction in model_predictions.values())
    strongest_malicious_signal = max(malicious_scores.values())
    extension_warning = has_extension_warning(ensemble_url)
    ensemble_host = (run_parse(ensemble_url).hostname or "").lower().rstrip(".")
    verified_hosts = {
        "google.com", "www.google.com", "google.co.in", "google.co.uk",
        "microsoft.com", "www.microsoft.com", "apple.com", "www.apple.com",
    }
    verified_host_override = ensemble_host in verified_hosts
    review_required = bool(
        not verified_host_override
        and (malicious_votes >= 2 or strongest_malicious_signal >= 0.80 or extension_warning)
    )
    if verified_host_override:
        final_prediction = "benign"
        final_confidence = 0.99
        final_source = "verified exact-host override after ensemble review"
    else:
        final_prediction = ensemble_prediction
        final_confidence = average_probabilities[ensemble_prediction]
        final_source = "four-model probability average"

    print(f"\nURL: {ensemble_url}")
    print("Individual model predictions:")
    for name in model_predictions:
        print(f"  {name}: {model_predictions[name]} | malicious score: {malicious_scores[name]:.2%}")
    print(f"Final prediction: {final_prediction}")
    print(f"Final confidence: {final_confidence:.2%}")
    print(f"Decision source: {final_source}")
    print(f"Strongest malicious signal: {strongest_malicious_signal:.2%}")
    print(f"Uncommon or typo-like extension: {extension_warning}")
    print(f"Security status: {'REVIEW / BLOCK' if review_required else 'LOWER RISK'}")
    print("Model confidence is an estimate, not proof that a website is safe.")

