/**
 * 应用入口文件
 */
import React, {useEffect, useState} from 'react';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {Provider} from 'react-redux';
import {PersistGate} from 'redux-persist/integration/react';
import {store, persistor} from '../store';
import {AppNavigator} from '../navigation';
import Loading from '../components/common/Loading';
import {storageService} from '../services/storage';

const App: React.FC = () => {
  const [storageReady, setStorageReady] = useState(false);

  useEffect(() => {
    let mounted = true;

    storageService
      .hydrate()
      .finally(() => {
        if (mounted) {
          setStorageReady(true);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  if (!storageReady) {
    return <Loading />;
  }

  return (
    <Provider store={store}>
      <PersistGate loading={<Loading />} persistor={persistor}>
        <SafeAreaProvider>
          <AppNavigator />
        </SafeAreaProvider>
      </PersistGate>
    </Provider>
  );
};

export default App;
