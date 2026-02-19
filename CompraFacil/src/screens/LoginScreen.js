import React, { useState } from 'react';
import { StyleSheet, View, Image, Alert } from 'react-native';
import { Text, Button, ActivityIndicator, Surface } from 'react-native-paper';
import { supabase } from '../lib/supabase';
import * as Google from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';

WebBrowser.maybeCompleteAuthSession();

export default function LoginScreen({ navigation }) {
  const [loading, setLoading] = useState(false);

  const [request, response, promptAsync] = Google.useAuthRequest({
    androidClientId: 'YOUR_ANDROID_CLIENT_ID',
    iosClientId: 'YOUR_IOS_CLIENT_ID',
    webClientId: 'YOUR_WEB_CLIENT_ID',
  });

  async function signInWithGoogle() {
    setLoading(true);
    try {
      const result = await promptAsync();
      if (result?.type === 'success') {
        const { authentication } = result;
        const { data, error } = await supabase.auth.signInWithIdToken({
          provider: 'google',
          token: authentication.idToken,
        });
        if (error) throw error;
      }
    } catch (error) {
      Alert.alert('Erro', error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Surface style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>CompraFácil</Text>
        <Text style={styles.subtitle}>Sua loja local em um só lugar</Text>
      </View>

      <View style={styles.content}>
        <Button
          mode="contained"
          onPress={signInWithGoogle}
          disabled={loading || !request}
          style={styles.button}
          contentStyle={styles.buttonContent}
          icon="google"
        >
          {loading ? <ActivityIndicator color="#fff" /> : 'Entrar com Google'}
        </Button>

        <Button
          mode="text"
          onPress={() => navigation.navigate('Home')}
          style={styles.skipButton}
        >
          Continuar sem login
        </Button>
      </View>
    </Surface>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
    backgroundColor: '#fff',
  },
  header: {
    alignItems: 'center',
    marginBottom: 50,
  },
  title: {
    fontSize: 36,
    fontWeight: 'bold',
    color: '#6200ee',
  },
  subtitle: {
    fontSize: 18,
    color: '#666',
    marginTop: 10,
    textAlign: 'center',
  },
  content: {
    width: '100%',
  },
  button: {
    borderRadius: 8,
    elevation: 2,
  },
  buttonContent: {
    height: 50,
  },
  skipButton: {
    marginTop: 15,
  },
});
