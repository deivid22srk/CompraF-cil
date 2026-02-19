import React, { useState, useEffect } from 'react';
import { View, StyleSheet, ScrollView, Image } from 'react-native';
import { TextInput, Button, HelperText, ActivityIndicator, Title } from 'react-native-paper';
import { useRouter, useLocalSearchParams } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { supabase } from '../src/lib/supabase';
import { decode } from 'base64-arraybuffer';

export default function AddProduct() {
  const router = useRouter();
  const { id } = useLocalSearchParams();
  const [loading, setLoading] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [image, setImage] = useState(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (id) {
      fetchProduct();
    }
  }, [id]);

  async function fetchProduct() {
    const { data, error } = await supabase.from('products').select('*').eq('id', id).single();
    if (data) {
      setName(data.name);
      setDescription(data.description);
      setPrice(data.price.toString());
      setImage(data.image_url);
    }
  }

  const pickImage = async () => {
    let result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.5,
      base64: true,
    });

    if (!result.canceled) {
      setImage(result.assets[0].uri);
      setBase64(result.assets[0].base64);
    }
  };

  const [base64, setBase64] = useState(null);

  async function uploadImage() {
    if (!base64) return image;

    setUploading(true);
    const fileName = `${Date.now()}.png`;
    const { data, error } = await supabase.storage
      .from('products')
      .upload(fileName, decode(base64), { contentType: 'image/png' });

    if (error) {
      console.error('Upload error:', error.message);
      setUploading(false);
      return image;
    }

    const { data: { publicUrl } } = supabase.storage.from('products').getPublicUrl(fileName);
    setUploading(false);
    return publicUrl;
  }

  async function handleSave() {
    setLoading(true);
    const imageUrl = await uploadImage();

    const productData = {
      name,
      description,
      price: parseFloat(price),
      image_url: imageUrl,
    };

    if (id) {
      const { error } = await supabase.from('products').update(productData).eq('id', id);
      if (error) console.error(error);
    } else {
      const { error } = await supabase.from('products').insert([productData]);
      if (error) console.error(error);
    }

    setLoading(false);
    router.back();
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <Title>{id ? 'Editar Produto' : 'Novo Produto'}</Title>

      <TextInput label="Nome do Produto" value={name} onChangeText={setName} style={styles.input} />
      <TextInput label="Preço (R$)" value={price} onChangeText={setPrice} keyboardType="numeric" style={styles.input} />
      <TextInput label="Descrição" value={description} onChangeText={setDescription} multiline numberOfLines={4} style={styles.input} />

      <Button mode="outlined" onPress={pickImage} style={styles.imageBtn} icon="camera">
        {image ? 'Alterar Foto' : 'Selecionar Foto'}
      </Button>

      {image && <Image source={{ uri: image }} style={styles.preview} />}

      {uploading && <ActivityIndicator animating={true} />}

      <Button mode="contained" onPress={handleSave} loading={loading} disabled={loading || uploading} style={styles.saveBtn}>
        Salvar Produto
      </Button>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  content: {
    padding: 20,
  },
  input: {
    marginBottom: 15,
  },
  imageBtn: {
    marginVertical: 10,
  },
  preview: {
    width: '100%',
    height: 200,
    borderRadius: 8,
    marginBottom: 20,
    resizeMode: 'cover',
  },
  saveBtn: {
    marginTop: 10,
    paddingVertical: 5,
  }
});
