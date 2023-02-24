# Busy Humans #
Busy Humans is a social media website that encourages people to learn and share real life skills through gamification. Users can define challenges for other users and reward their learning progress with virtual experience points.

**Try the official online version: [busyhumans.woizischke.com](http://busyhumans.woizischke.com)**

Use IntelliJ IDEA to build and Dockerfile to deploy.

<details>
<summary>How to install manually</summary>

```sh
# setup mongodb repo
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
sudo vi /etc/apt/sources.list.d/10gen.list
deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen
:wq

# upgrade system
sudo apt-get update
sudo apt-get upgrade

# setup time sync
sudo apt-get install ntp

# install java
sudo apt-get install openjdk-7-jdk

# install tomcat
sudo apt-get install tomcat7
sudo apt-get install tomcat7-admin

# setup tomcat dns cache timeout (otherwise it's infinite which is bad for recaptcha)
sudo vi /usr/share/tomcat7/bin/setenv.sh
export JAVA_OPTS="$JAVA_OPTS -Dsun.net.inetaddr.ttl=30"
:wq

# setup tomcat admin credentials
sudo vi /var/lib/tomcat7/conf/tomcat-users.xml
  <role rolename="admin-gui"/>
  <role rolename="manager-gui"/>
  <user username="{{admin_username}}" password="{{admin_password}}" roles="admin-gui,manager-gui"/>
:wq

# set maximum war size
sudo vi /usr/share/tomcat7-admin/manager/WEB-INF/web.xml
      <max-file-size>104857600</max-file-size>
      <max-request-size>104857600</max-request-size>
:wq

# apply tomcat configuration
sudo service tomcat7 restart

# remove default root webapp
sudo rm -R /var/lib/tomcat7/webapps/ROOT

# route port 8080 to 80
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
sudo iptables-save > /root/port80.fw
sudo vi /etc/network/interfaces
# The primary network interface
auto eth0
iface eth0 inet dhcp
    post-up iptables-restore < /root/port80.fw
:wq

# install mongodb
sudo apt-get install mongodb-10gen

# setup mongodb security
# WARNING: use the keyFile option for sharding instead
# see: http://www.mongodb.org/display/DOCS/Security+and+Authentication#SecurityandAuthentication-MongoSecurity
mongo
use admin
db.addUser("mastery-admin", "{{db_password}}")
use mastery
db.addUser("mastery-app", "{{service_password}}")
exit
sudo vi /etc/mongodb.conf
auth = true
:wq
sudo service mongodb restart

#
# setup email
#
sudo apt-get install postfix
#Select configuration: Internet Site
#System mail name: {{hostname}}
sudo vi /etc/postfix/domains
{{hostname}} OK
:wq
sudo vi /etc/postfix/virtual
{{mail_user}}@{{hostname}} {{mail_receiver}}
@{{hostname}} {{mail_receiver}}
:wq
sudo vi /etc/postfix/main.cf
myhostname = {{hostname}}
# add {{hostname}} to mydestination: mydestination = {{hostname}}, [...]
# Virtual Mailbox Domain Settings
virtual_alias_domains = hash:/etc/postfix/domains
virtual_alias_maps = hash:/etc/postfix/virtual
virtual_mailbox_limit = 64000
virtual_minimum_uid = 5000
virtual_uid_maps = static:5000
virtual_gid_maps = static:5000
virtual_mailbox_base = /home/vmail
virtual_transport = virtual
# Security
disable_vrfy_command = yes
smtpd_sasl_auth_enable = yes
smtpd_sasl_path = smtpd
broken_sasl_auth_clients = yes
smtpd_delay_reject = yes
smtpd_helo_required = yes
smtpd_helo_restrictions = 
	permit_mynetworks, 
	permit_sasl_authenticated, 
	reject_unauth_destination
smtpd_recipient_restrictions =
	permit_sasl_authenticated,
	permit_mynetworks,
	reject_invalid_hostname,
	reject_non_fqdn_hostname,
	reject_unauth_pipelining,
	reject_non_fqdn_hostname,  
	reject_non_fqdn_sender,
	reject_non_fqdn_recipient,
	reject_unauth_destination,
	reject_unknown_sender_domain,
	reject_unknown_recipient_domain,
	reject_rbl_client sbl.spamhaus.org,
	reject_rbl_client cbl.abuseat.org,
	reject_rbl_client dul.dnsbl.sorbs.net,
	permit
smtpd_error_sleep_time = 1s
smtpd_soft_error_limit = 10
smtpd_hard_error_limit = 20
:wq
sudo mkdir /home/vmail
sudo sudo chown postfix /home/vmail
sudo sudo chgrp postfix /home/vmail
sudo postmap /etc/postfix/domains
sudo postmap /etc/postfix/virtual
sudo apt-get install sasl2-bin
sudo vi /etc/default/saslauthd
START=yes
MECHANISMS="shadow"
:wq
sudo vi /etc/postfix/sasl/smtpd.conf
pwcheck_method: saslauthd
mech_list: PLAIN LOGIN
saslauthd_path: /var/run/saslauthd/mux
:wq
sudo adduser postfix sasl
sudo vi /etc/postfix/master.cf
smtp      inet  n       -       n       -       -       smtpd
smtps     inet  n       -       n       -       -       smtpd -o smtpd_tls_wrappermode=yes -o smtpd_sasl_auth_enable=yes
:wq
sudo service saslauthd restart
sudo service postfix restart

#
# mail user
#
sudo adduser {{mail_user}}
password: {{mail_password}}
name: Busy Humans

#
# setup solr
#
#use prepared solr.war from svn or:
#download zip from http://lucene.apache.org/solr/
#extract dist/?.war
#rename ?.war to solr.war
#deploy war via tomcat manager
sudo service tomcat7 stop
#upload svn/solr_home to /home/ubuntu/solr_home
sudo chmod ugo+x /home/ubuntu/solr_home/deploy.sh
sudo /home/ubuntu/solr_home/deploy.sh
sudo vi /var/lib/tomcat7/conf/Catalina/localhost/solr.xml
<?xml version="1.0" encoding="UTF-8"?>
<Context path="/solr">
    <Valve className="org.apache.catalina.valves.RemoteAddrValve" allow="127.0.0.1"/>
    <Environment name="solr/home" type="java.lang.String" value="/var/lib/solr" overrides="true"/>
</Context>
:wq
sudo service tomcat7 start

#
# setup monit
#
sudo apt-get install monit

sudo vi /etc/monit/conf.d/general
set mailserver localhost
set mail-format { from: root@{{hostname}} }
set alert alert@{{hostname}}
set httpd port 2812 and
    allow {{admin_username}}:{{admin_password}}
:wq

sudo vi /etc/monit/conf.d/system
check system {{hostname}}
    if loadavg (1min) > 4 then alert
    if loadavg (5min) > 2 then alert
    if memory usage > 75% then alert
    if swap usage > 25% then alert
    if cpu usage (user) > 70% then alert
    if cpu usage (system) > 30% then alert
    if cpu usage (wait) > 20% then alert
:wq

sudo vi /etc/monit/conf.d/web
check host {{hostname}} with address {{hostname}}
    if failed url http://{{hostname}} then alert
:wq

#
# optional
#

# stop the popularity-contests job because it sends daily error mails to root@{{hostname}}
sudo chmod -x /etc/cron.daily/popularity-contest

# create an upstart log directory because /etc/cron.daily/logrotate needs it
sudo mkdir /var/log/upstart
```
</details><p></p>
