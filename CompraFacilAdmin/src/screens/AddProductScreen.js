import React, { useState } from 'react';
import { StyleSheet, View, ScrollView, Image, Alert } from 'react-native';
import { TextInput, Button, Title, HelperText, ActivityIndicator, Surface, Text } from 'react-native-paper';
import * as ImagePicker from 'expo-image-picker';
import { supabase } from '../lib/supabase';
import { decode } from 'base64-arraybuffer';

export default function AddProductScreen({ route, navigation }) {
  const editingProduct = route.params?.product;

  const [name, setName] = useState(editingProduct?.name || '');
  const [description, setDescription] = useState(editingProduct?.description || '');
  const [price, setPrice] = useState(editingProduct?.price?.toString() || '');
  const [image, setImage] = useState(editingProduct?.image_url || null);
  const [imageBase64, setImageBase64] = useState(null);
  const [uploading, setUploading] = useState(false);

  const pickImage = async () => {
    let result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.7,
      base64: true,
    });

    if (!result.canceled) {
      setImage(result.assets[0].uri);
      setImageBase64(result.assets[0].base64);
    }
  };

  async function handleSave() {
    if (!name || !price) {
      Alert.alert('Erro', 'Nome e preço são obrigatórios.');
      return;
    }

    setUploading(true);
    try {
      let imageUrl = image;

      if (imageBase64) {
        const fileName = `${Date.now()}.jpg`;
        const { error: uploadError } = await supabase.storage
          .from('product-images')
          .upload(fileName, decode(imageBase64), {
            contentType: 'image/jpeg'
          });

        if (uploadError) throw uploadError;

        const { data: publicUrlData } = supabase.storage
          .from('product-images')
          .getPublicUrl(fileName);

        imageUrl = publicUrlData.publicUrl;
      }

      const productData = {
        name,
        description,
        price: parseFloat(price),
        image_url: imageUrl,
      };

      if (editingProduct) {
        const { error } = await supabase
          .from('products')
          .update(productData)
          .eq('id', editingProduct.id);
        if (error) throw error;
      } else {
        const { error } = await supabase
          .from('products')
          .insert([productData]);
        if (error) throw error;
      }

      Alert.alert('Sucesso', 'Produto salvo com sucesso!');
      navigation.goBack();
    } catch (error) {
      Alert.alert('Erro', error.message);
    } finally {
      setUploading(false);
    }
  }

  return (
    <ScrollView style={styles.container}>
      <Surface style={styles.form}>
        <Title style={styles.formTitle}>{editingProduct ? 'Editar Produto' : 'Novo Produto'}</Title>

        <TextInput
          label="Nome do Produto"
          value={name}
          onChangeText={setName}
          mode="outlined"
          style={styles.input}
        />

        <TextInput
          label="Descrição"
          value={description}
          onChangeText={setDescription}
          mode="outlined"
          multiline
          numberOfLines={4}
          style={styles.input}
        />

        <TextInput
          label="Preço (R$)"
          value={price}
          onChangeText={setPrice}
          mode="outlined"
          keyboardType="numeric"
          style={styles.input}
          left={<TextInput.Affix text="R$ " />}
        />

        <Title style={styles.label}>Imagem do Produto</Title>
        <View style={styles.imageContainer}>
          {image ? (
            <Image source={{ uri: image }} style={styles.preview} />
          ) : (
            <View style={styles.placeholder}>
              <Text>Nenhuma imagem selecionada</Text>
            </View>
          )}
          <Button mode="outlined" onPress={pickImage} style={styles.pickButton}>
            {image ? 'Trocar Imagem' : 'Selecionar Imagem'}
          </Button>
        </View>

        <Button
          mode="contained"
          onPress={handleSave}
          disabled={uploading}
          style={styles.saveButton}
          contentStyle={styles.saveButtonContent}
          buttonColor="#28a745"
        >
          {uploading ? <ActivityIndicator color="#fff" /> : 'Salvar Produto'}
        </Button>
      </Surface>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f6f6f6',
  },
  form: {
    margin: 15,
    padding: 20,
    borderRadius: 15,
    elevation: 4,
  },
  formTitle: {
    marginBottom: 20,
    textAlign: 'center',
  },
  input: {
    marginBottom: 15,
  },
  label: {
    fontSize: 16,
    marginTop: 10,
    marginBottom: 10,
  },
  imageContainer: {
    alignItems: 'center',
    marginBottom: 20,
  },
  preview: {
    width: '100%',
    height: 200,
    borderRadius: 10,
  },
  placeholder: {
    width: '100%',
    height: 200,
    borderRadius: 10,
    backgroundColor: '#eee',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#ccc',
    borderStyle: 'dashed',
  },
  pickButton: {
    marginTop: 10,
  },
  saveButton: {
    marginTop: 10,
    borderRadius: 8,
  },
  saveButtonContent: {
    height: 50,
  },
});
