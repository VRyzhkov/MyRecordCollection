using MyRecordCollection.Models;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Linq;
using System.Runtime.CompilerServices;

namespace MyRecordCollection.ViewModels
{
    public class MyCollectionViewModel : INotifyPropertyChanged
    {

        public ObservableCollection<Composer> MyCollection { get; private set; } = new ObservableCollection<Composer>();
        public IList<Composer> MyEmptyCollection { get; private set; }

        public Composer PreviousComposer { get; set; }
        public Composer CurrentComposer { get; set; }
        public Composer CurrentItem { get; set; }
        public int PreviousPosition { get; set; }
        public int CurrentPosition { get; set; }
        public int Position { get; set; }

        public MyCollectionViewModel()
        {
            MyCollection = GenerateCollection();

            CurrentItem = MyCollection.Skip(3).FirstOrDefault();
            OnPropertyChanged("CurrentItem");
            Position = 3;
            OnPropertyChanged("Position");
        }

        static ObservableCollection<Composer> GenerateCollection()
        {
            var rnd = new Random();
            var result = new ObservableCollection<Composer>
            {
                new Composer
                {
                    Id = Guid.Empty,
                    Title = "*** ALL ***",
                    Plates = new List<Plate>(),
                    ImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Sprechender_Brief_--_2015_--_6008.jpg/274px-Sprechender_Brief_--_2015_--_6008.jpg"
                }
            };

            for (int i = 0; i < 20; i++)
            {
                var composer = new Composer
                {
                    Id = Guid.NewGuid(),
                    Title = (Guid.NewGuid()).ToString().Substring(0, 6),
                    Plates = new List<Plate>(),
                    ImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Sprechender_Brief_--_2015_--_6008.jpg/274px-Sprechender_Brief_--_2015_--_6008.jpg"
                };

                composer.Plates.Add(new Plate
                {
                    Id = Guid.Empty,
                    Title = "*** ALL ***",
                    ImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Sprechender_Brief_--_2015_--_6008.jpg/274px-Sprechender_Brief_--_2015_--_6008.jpg",
                        Year = rnd.Next(1980, 2023),
                    Tracks = new List<Track>(),
                });

                for (int j = 0; j < rnd.Next(2, 12); j++)
                {
                    var plate = new Plate
                    {
                        Id = Guid.NewGuid(),
                        Title = (Guid.NewGuid()).ToString().Substring(0, 6),
                        ImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/64/Sprechender_Brief_--_2015_--_6008.jpg/274px-Sprechender_Brief_--_2015_--_6008.jpg",
                        Year = rnd.Next(1980, 2023),
                        Tracks = new List<Track>(),
                    };
                    for (int k = 0; k < rnd.Next(3, 12); k++)
                        plate.Tracks.Add(
                            new Track
                            {
                                Id = Guid.NewGuid(),
                                Title = (Guid.NewGuid()).ToString().Substring(0, 6),
                                Number = k + 1
                            });
                    composer.Plates.Add(plate);
                };
                result.Add(composer);
            }

            return result;
        }

        #region INotifyPropertyChanged

        public event PropertyChangedEventHandler PropertyChanged;

        void OnPropertyChanged([CallerMemberName] string propertyName = null)
        {
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
        }

        #endregion
    }
}
