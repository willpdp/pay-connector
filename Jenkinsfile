#!/usr/bin/env groovy

pipeline {
  agent any

  options {
    ansiColor('xterm')
    timestamps()
  }

  libraries {
    lib("pay-jenkins-library@master")
  }

  environment {
    DOCKER_HOST = "unix:///var/run/docker.sock"
  }

  stages {
    stage('Maven Build') {
      steps {
        sh 'docker pull govukpay/postgres:9.4.4'
        sh 'mvn clean package'
      }
    }
    stage('Docker Build') {
      steps {
        script {
          buildApp{
            app = "connector"
          }
        }
      }
    }
    stage('Test') {
      steps {
        runEndToEnd("connector")
      }
    }
    stage('Docker Tag') {
      steps {
        script {
          dockerTag {
            app = "connector"
          }
        }
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        deploy("connector", "test", null, true)
      }
    }
  }
}
