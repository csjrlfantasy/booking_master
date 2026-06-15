import React, {useCallback, useEffect, useState} from 'react';
import {
  Alert,
  FlatList,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Colors, Typography, Spacing, BorderRadius, Shadows} from '../../constants';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import {userApi, type UserIdentity} from '../../services/api/userApi';

const IdentityScreen: React.FC = () => {
  const [identities, setIdentities] = useState<UserIdentity[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [realName, setRealName] = useState('');
  const [idCardNo, setIdCardNo] = useState('');
  const [phone, setPhone] = useState('');

  const loadIdentities = useCallback(async () => {
    setLoading(true);
    try {
      const data = await userApi.getIdentities();
      setIdentities(data || []);
    } catch (error: any) {
      Alert.alert('加载失败', error?.message || '请先登录后查看常用观演人');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadIdentities();
  }, [loadIdentities]);

  const resetForm = () => {
    setRealName('');
    setIdCardNo('');
    setPhone('');
  };

  const handleCreate = async () => {
    const normalizedId = idCardNo.trim().toUpperCase();
    if (!realName.trim() || !normalizedId) {
      Alert.alert('请完善信息', '真实姓名和身份证号不能为空');
      return;
    }

    setSaving(true);
    try {
      await userApi.createIdentity({
        realName: realName.trim(),
        idCardNo: normalizedId,
        phone: phone.trim() || undefined,
      });
      resetForm();
      await loadIdentities();
      Alert.alert('保存成功', '常用观演人已保存');
    } catch (error: any) {
      Alert.alert('保存失败', error?.message || '身份证号不合法或已存在');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = (identity: UserIdentity) => {
    Alert.alert('删除观演人', `确定删除 ${identity.realName} 吗？`, [
      {text: '取消', style: 'cancel'},
      {
        text: '删除',
        style: 'destructive',
        onPress: async () => {
          try {
            await userApi.deleteIdentity(identity.id);
            await loadIdentities();
          } catch (error: any) {
            Alert.alert('删除失败', error?.message || '请稍后再试');
          }
        },
      },
    ]);
  };

  const renderIdentity = ({item}: {item: UserIdentity}) => (
    <View style={[styles.identityCard, Shadows.sm]}>
      <View style={styles.identityMain}>
        <Text style={styles.identityName}>{item.realName}</Text>
        <Text style={styles.identityId}>{item.maskedIdCardNo || item.idCardNo}</Text>
        {item.phone ? <Text style={styles.identityPhone}>{item.phone}</Text> : null}
      </View>
      <TouchableOpacity style={styles.deleteButton} onPress={() => handleDelete(item)}>
        <Text style={styles.deleteText}>删除</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <SafeAreaView style={styles.container} edges={['bottom']}>
      {loading && identities.length === 0 ? <Loading fullscreen text="加载中..." /> : null}

      <FlatList
        data={identities}
        keyExtractor={(item) => String(item.id)}
        renderItem={renderIdentity}
        ListHeaderComponent={(
          <View style={[styles.formCard, Shadows.sm]}>
            <Text style={styles.title}>常用观演人</Text>
            <Text style={styles.tip}>购票实名制会使用这里保存的身份证信息。</Text>

            <Text style={styles.label}>真实姓名</Text>
            <TextInput
              value={realName}
              onChangeText={setRealName}
              placeholder="请输入真实姓名"
              style={styles.input}
            />

            <Text style={styles.label}>身份证号</Text>
            <TextInput
              value={idCardNo}
              onChangeText={setIdCardNo}
              placeholder="18位居民身份证号"
              autoCapitalize="characters"
              style={styles.input}
            />

            <Text style={styles.label}>手机号</Text>
            <TextInput
              value={phone}
              onChangeText={setPhone}
              placeholder="选填"
              keyboardType="phone-pad"
              style={styles.input}
            />

            <Button
              title="保存观演人"
              loading={saving}
              fullWidth
              onPress={handleCreate}
              style={styles.saveButton}
            />
          </View>
        )}
        ListEmptyComponent={!loading ? (
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>暂无常用观演人</Text>
            <Text style={styles.emptyTip}>先添加身份证，购票时就能直接选择。</Text>
          </View>
        ) : null}
        refreshing={loading}
        onRefresh={loadIdentities}
        contentContainerStyle={styles.content}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.gray[100],
  },
  content: {
    padding: Spacing.md,
  },
  formCard: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    marginBottom: Spacing.md,
  },
  title: {
    fontSize: Typography.h4.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  tip: {
    marginTop: Spacing.xs,
    marginBottom: Spacing.md,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
  label: {
    marginTop: Spacing.sm,
    marginBottom: Spacing.xs,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[700],
    fontWeight: '600',
  },
  input: {
    height: 44,
    borderWidth: 1,
    borderColor: Colors.gray[200],
    borderRadius: BorderRadius.sm,
    paddingHorizontal: Spacing.md,
    fontSize: Typography.body.fontSize,
    color: Colors.gray[900],
    backgroundColor: Colors.white,
  },
  saveButton: {
    marginTop: Spacing.base,
  },
  identityCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    marginBottom: Spacing.sm,
  },
  identityMain: {
    flex: 1,
  },
  identityName: {
    fontSize: Typography.body.fontSize,
    fontWeight: '700',
    color: Colors.gray[900],
  },
  identityId: {
    marginTop: 4,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[600],
  },
  identityPhone: {
    marginTop: 2,
    fontSize: Typography.caption.fontSize,
    color: Colors.gray[500],
  },
  deleteButton: {
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.xs,
  },
  deleteText: {
    color: Colors.error[500],
    fontSize: Typography.bodySmall.fontSize,
    fontWeight: '600',
  },
  empty: {
    alignItems: 'center',
    paddingVertical: Spacing.xl,
  },
  emptyTitle: {
    fontSize: Typography.body.fontSize,
    color: Colors.gray[700],
    fontWeight: '600',
  },
  emptyTip: {
    marginTop: Spacing.xs,
    fontSize: Typography.bodySmall.fontSize,
    color: Colors.gray[500],
  },
});

export default IdentityScreen;
